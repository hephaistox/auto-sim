(ns auto-sim.rc
  "Models the resource consumers interaction.

  Definitions:
  * Resource definition: A limited quantity of items that are seized and disposed by entities as they proceed through the system. A resource has a capacity that governs the total quantity of items that may be available. All the items in the resource are homogeneous, meaning that they are indistinguishable. If an entity attempts to seize a resource that does not have any units available it must wait in a queue. It is often representing real world items that availability is limited (e.g. machine, wrench).
  * Consumer definition: A consumer is responsible for seizing and disposing the resource.

  A resource defines:
  * `capacity` (default 1) total number of available resources, note that this number may evolve over time.
  * `consumption` (default []) list of events currently consumption this resource, is useful to track them and enabling preemption and failures. The postponable-event contains the information on the waiting entity.
  * `queue` (default []) list of blocked entities.
  * `renewable?` (default true) when true, the disposing is not giving back the values."
  (:require
   [auto-sim.engine      :as-alias sim-engine]
   [auto-sim.entity      :as sim-entity]
   [auto-sim.rc.resource :as sim-rc-resource]))

(defn nb-consumed-resources
  "Returned how much resources `resource-name` are currently consumed"
  [event-return _event _bucket resource-name]
  (let [resource (get-in event-return [::sim-engine/state ::sim-engine/resource resource-name])]
    (sim-rc-resource/nb-consumed-resources resource)))

(defn nb-available-resources
  "Returned how much resources `resource-name` are available"
  [event-return _event _bucket resource-name]
  (let [resource (get-in event-return [::sim-engine/state ::sim-engine/resource resource-name])]
    (sim-rc-resource/nb-available-resources resource)))

(defn define-resource
  "Update `state` to define `resource-name` with `resource`.

  Note that `resource` is defaulted if necessary."
  [event-return _event _bucket resource-name resource]
  (update event-return
          ::sim-engine/state
          assoc-in
          [::sim-engine/resource resource-name]
          (sim-rc-resource/defaulting-values resource)))

(defn seize
  "The `postponable-event` is executed when the resource called `resource-name` has `quantity` items available.

  There are two cases:
  * If resources are lacking, the consumption is postponed, and the `postponable-event` stored in the `queue` of the `resource`. It may be triggered latter on, when freeing or capacity updating.
  * If resources are sufficient, a consumption is added in the `resource`, the `postponable-event` is planned to be executed now by adding it in the `future-events`.

  The `priority` defines.

  Returns a map:
  * `state`
  * `future-events`"
  [event-return event bucket resource-name quantity postponable-event priority]
  (if-let [resource (get-in event-return [::sim-engine/state ::sim-engine/resource resource-name])]
    (let [{:keys [consumption-uuid resource errors]}
          (sim-rc-resource/seize resource event quantity priority postponable-event)]
      (cond-> (-> event-return
                  (assoc-in [::sim-engine/state ::sim-engine/resource resource-name] resource))
        consumption-uuid (sim-entity/schedule event bucket postponable-event)
        (seq errors) (update ::sim-engine/errors #(reduce (fnil conj []) % errors))))
    ;;NOTE seizing is noop
    (update event-return
            ::sim-engine/errors
            (fnil conj [])
            #::sim-engine{:why :resource-not-found
                          :resource-name resource-name
                          :quantity quantity
                          :possible-resources (-> event-return
                                                  ::sim-engine/state
                                                  ::sim-engine/resource
                                                  keys
                                                  vec)})))

(defn dispose
  "Dispose a resource called `resource-name` for entity of `postponable-event`.

  The new available resources can be used by some other entities. `unqueueing-policy-fn` is used to select events to unqueue.

  Returns a pair:
  * `state`
  * `future-events`"
  [event-return event bucket resource-name quantity unqueueing-policy-fn priority-comp]
  (if-let [resource (get-in event-return [::sim-engine/state ::sim-engine/resource resource-name])]
    (let [{:keys [events resource errors]}
          (sim-rc-resource/dispose resource priority-comp unqueueing-policy-fn quantity)]
      (cond-> event-return
        resource (assoc-in [::sim-engine/state ::sim-engine/resource resource-name] resource)
        (seq events) (sim-entity/schedule-events event bucket (mapv ::sim-engine/event events))
        (seq errors) (update ::sim-engine/errors #(reduce (fnil conj []) % errors))))
    (update event-return
            ::sim-engine/errors
            (fnil conj [])
            #::sim-engine{:why :resource-not-found
                          :resource-name resource-name
                          :quantity quantity})))

(defn update-capacity
  "Update the resource capacity."
  [event-return event bucket resource-name new-capacity preemption-policy-fn unqueueing-policy-fn]
  (if-let [resource (get-in event-return [::sim-engine/state ::sim-engine/resource resource-name])]
    (let [{:keys [events _preempt resource]} (sim-rc-resource/update-capacity resource
                                                                              preemption-policy-fn
                                                                              unqueueing-policy-fn
                                                                              new-capacity)]
      (cond-> event-return
        resource (assoc-in [::sim-engine/state ::sim-engine/resource resource-name] resource)
        events (sim-entity/schedule-events event bucket events)))
    (update event-return
            ::sim-engine/errors
            (fnil conj [])
            #::sim-engine{:why :resource-not-found
                          :resource-name resource-name
                          :new-capacity new-capacity})))
