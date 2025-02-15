(ns auto-sim.rc
  "Models resource consumers interaction.

  Resource definition:
  * A limited quantity of items that are used (e.g. seized and disposed) by entities as they proceed through the system. A resource has a capacity that governs the total quantity of items that may be available. All the items in the resource are homogeneous, meaning that they are indistinguishable. If an entity attempts to seize a resource that does not have any units available it must wait in a queue. It is often representing real world items that availability is limited (e.g. machine, wrench).

  Consumer definition:
  * A consumer is responsible for seizing and disposing the resource."
  (:require
   [auto-sim.engine      :as-alias sim-engine]
   [auto-sim.rc.resource :as sim-rc-resource]
   [auto-sim.rc.state    :as sim-rc-state]))

(defn define-resource
  "Update `state` to define `resource-name` with `resource`.

  Note that `resource` is defaulted if necessary."
  [state resource-name resource]
  (assoc-in state [::resource resource-name] (sim-rc-resource/defaulting-values resource)))

(defn seize
  "The `event` would be executed when the resource called `resource-name` will be available for `quantity`.

  There are two cases:
  * If resources are lacking, the consumption is postpone, and the `event` stored in the `queue` of the `resource`. It may be triggered latter on, by a freeing or capacity update.
  * If resources are sufficient, a consumption is added in the `resource`, the `event` is planned to be executed now by adding it in the `future-events`.

  Returns a pair:
  * `state`
  * `future-events`"
  [state future-events resource-name quantity seizing-bucket event]
  (let [resource (get-in state [::resource resource-name])]
    (if (or (nil? resource) (nil? event))
      [state future-events] ;;NOTE seizing is noop
      (let [event (-> event
                      (assoc ::sim-engine/bucket seizing-bucket))
            [consumption-uuid resource] (sim-rc-resource/seize resource quantity event)
            state (assoc-in state [::resource resource-name] resource)]
        (if (nil? consumption-uuid)
          ;;NOTE event is postponed, as no resource is available.
          [state future-events]
          ;;NOTE in this case, the consumption is started, event could be started
          (let [event (assoc-in event [::resource resource-name] consumption-uuid)]
            [state (conj future-events event)]))))))

(defn dispose
  "Dispose the resources consumed by `event`, the new availability may create some new seizing.

  Returns the `event-return` with the resource disposed, so it is available again.

  A consumer is unblocked, the capacity of `resource-name` is freed."
  [state future-events resource-name event]
  (let [{::keys [resource]
         ::sim-engine/keys [bucket]}
        event
        [unblockings state] (sim-rc-state/dispose state resource-name (get resource resource-name))]
    (reduce (fn [[state future-events]
                 {::keys [quantity seizing-event]
                  :as _blocking}]
              (seize state future-events resource-name quantity bucket seizing-event))
            [state future-events]
            unblockings)))

(defn resource-update
  "Update the resource capacity."
  [{::sim-engine/keys [state]
    :as event-return}
   resource-name
   new-capacity]
  (let [[unblocked-events state]
        (sim-rc-state/update-resource-capacity state resource-name new-capacity)]
    (-> event-return
        (assoc ::sim-engine/state state)
        (sim-event-return/add-events unblocked-events))))

(defn wrap-model
  "Wraps a model to add necessary behavior to model a resource/consumer.

  Resource/Consumer modeling is a way to model state and events for simulation, by using concepts of resource being used by consumer

  The `resources` is a map defining the resource available:
      * `policy` In a queue, the policy selects the next consumer that will be unblocked. (Each queue has its own policy)
      * `renewable?` When disposed, a renewable resource model is available again. Typically the toolings like wrenches, hammers, machines are most often renewable resources."
  [{{::keys [rc]} ::sim-engine/model-data
    :as model}
   unblocking-policy-registry
   preemption-policy-registry]
  (cond-> model
    (seq rc) (update-in [::sim-engine/initial-snapshot ::sim-engine/state]
                        (fn [state] (sim-rc-state/define-resources state rc)))))
