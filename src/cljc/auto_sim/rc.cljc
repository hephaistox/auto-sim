(ns auto-sim.rc
  "Models the resource consumers interaction.

  Definitions:
  * Resource definition: A limited quantity of items that are seized and disposed by entities as they proceed through the system. A resource has a capacity that governs the total quantity of items that may be available. All the items in the resource are homogeneous, meaning that they are indistinguishable. If an entity attempts to seize a resource that does not have any units available it must wait in a queue. It is often representing real world items that availability is limited (e.g. machine, wrench).
  * Consumer definition: A consumer is responsible for seizing and disposing the resource.

  A resource defines:
  * `capacity` (default 1) total number of available resources, note that this number may evolve over time.
  * `consumption` (default []) list of events currently consumption this resource, is useful to track them and enabling preemption and failures. The event contains the information on the waiting entity.
  * `queue` (default []) list of blocked entities.
  * `renewable?` (default true) when true, the disposing is not giving back the values."
  (:require
   [auto-sim.engine      :as-alias sim-engine]
   [auto-sim.rc.resource :as sim-rc-resource]))

(defn define-resource
  "Update `state` to define `resource-name` with `resource`.

  Note that `resource` is defaulted if necessary."
  [state resource-name resource]
  (-> state
      (assoc-in [::sim-engine/resource resource-name]
                (sim-rc-resource/defaulting-values resource))))

(defn seize
  "The `event` is executed when the resource called `resource-name` has `quantity` items available.

  There are two cases:
  * If resources are lacking, the consumption is postponed, and the `event` stored in the `queue` of the `resource`. It may be triggered latter on, by a freeing or capacity update.
  * If resources are sufficient, a consumption is added in the `resource`, the `event` is planned to be executed now by adding it in the `future-events`.

  Returns a map:
  * `state`
  * `future-events`"
  [state future-events seizing-bucket event resource-name quantity]
  (let [resource (get-in state [::sim-engine/resource resource-name])]
    (if (or (nil? resource) (nil? event))
      ;;NOTE seizing is noop
      {:state state
       ;;TODO event should be used in stopping-criteria
       :event (assoc event
                     ::sim-engine/errors
                     #::sim-engine{:resource-name resource-name
                                   :quantity quantity
                                   :seizing-bucket seizing-bucket
                                   :possible-resources (-> state
                                                           ::sim-engine/resource
                                                           keys
                                                           vec)})
       :future-events future-events}
      (let [event (-> event
                      (assoc ::sim-engine/bucket seizing-bucket))
            {:keys [consumption-uuid resource errors]}
            (sim-rc-resource/seize resource quantity event)
            state (assoc-in state [::sim-engine/resource resource-name] resource)
            event (cond-> event
                    errors (assoc ::sim-engine/errors errors))]
        (if (nil? consumption-uuid)
          ;;NOTE event is postponed, as no resource is available.
          {:state state
           :event (assoc event ::sim-engine/errors #::sim-engine{:why :no-resource-available})
           :future-events future-events}
          ;;NOTE in this case, the consumption is started, event could be started
          {:state state
           :future-events (->> consumption-uuid
                               (assoc-in event [::sim-engine/resource resource-name])
                               (conj future-events))})))))

(defn dispose
  "Dispose a resource called `resource-name` for entity of `event`.

  The new available resources can be used by some other entities. `unqueueing-policy-fn` is used to select events to unqueue.

  Returns a pair:
  * `state`
  * `future-events`"
  [state future-events event resource-name unqueueing-policy-fn]
  ;;TODO Remove unblocking that should be stored somewhere
  (if (nil? resource-name)
    [[] state]
    (let [{::sim-engine/keys [bucket entity-id]} event
          {:keys [unqueued resource]}
          (-> state
              (get-in [::sim-engine/resource resource-name])
              (sim-rc-resource/dispose entity-id unqueueing-policy-fn nil 1))
          state (assoc-in state [::sim-engine/resource resource-name] resource)]
      (reduce (fn [[state future-events]
                   {::sim-engine/keys [quantity event]
                    :as _blocking}]
                (seize state future-events resource-name quantity bucket event))
              [state future-events]
              unqueued))))

(defn resource-update
  "Update the resource capacity."
  [state future-events resource-name new-capacity preemption-policy-fn unqueueing-policy-fn]
  (let [resource (get-in state [::sim-engine/resource resource-name])
        [unblocked-events resource] (sim-rc-resource/update-capacity resource
                                                                     new-capacity
                                                                     preemption-policy-fn
                                                                     unqueueing-policy-fn)]
    [unblocked-events
     (assoc-in state [::sim-engine/resource resource-name] resource)
     (update future-events ::sim-engine/future-events concat unblocked-events)]))
