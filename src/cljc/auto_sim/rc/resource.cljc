(ns auto-sim.rc.resource
  "This namespace is assembling the consumption and queue logics."
  (:require
   [auto-sim.engine                  :as-alias sim-engine]
   [auto-sim.entity                  :as sim-entity]
   [auto-sim.rc.resource.consumption :as sim-rc-consumption]
   [auto-sim.rc.resource.queue       :as sim-rc-queue]))

(defn defaulting-values
  "Returns a resource with default values added."
  [{::sim-engine/keys [capacity consumption queue renewable?]
    :as resource
    :or {capacity 1
         consumption {}
         queue []
         renewable? true}}]
  (assoc resource
         ::sim-engine/capacity capacity
         ::sim-engine/consumption consumption
         ::sim-engine/queue queue
         ::sim-engine/renewable? renewable?))

(defn nb-consumed-resources
  "Returns the number of consumed resources."
  [{::sim-engine/keys [consumption]
    :as _resource}]
  (if (map? consumption)
    (->> consumption
         vals
         (map (fn [{::sim-engine/keys [consumption-quantity]
                    :or {consumption-quantity 1}}]
                consumption-quantity))
         (apply + 0))
    0))

(defn nb-available-resources
  "Returns the number of available resources based on the defined `capacity,` and the `consumption` resources (i.e. sum of their )."
  [{::sim-engine/keys [capacity]
    :or {capacity 1}
    :as resource}]
  (max 0 (- capacity (nb-consumed-resources resource))))

(defn seize
  "If a `resource` contains enough available items, a `consumption` is created and stored in the `resource`.
  Otherwise, no `consumption` is created and the `event` is queued in the `resource`, to test the seizing later on.

  In any case, it returns a map with:
  * `consumption-uuid` or `nil` if the execution is postponed awaiting for a resource disposal
  *  updated `resource` reflecting the consumption
  * `errors`"
  [resource event consumption-quantity priority postponable-event]
  (if (>= (compare (nb-available-resources resource) consumption-quantity) 0)
    (sim-rc-consumption/start resource event consumption-quantity priority)
    (sim-rc-queue/queue-event resource
                              (sim-entity/copy-entity-id event postponable-event)
                              consumption-quantity
                              priority)))

(defn- unqueue-attempts
  "Try to unqueue events regarding the newly available items.

  Returns a map with:
  * `resource` updated
  * `events` list of event to add in the `future-events` to try their execution again."
  [resource unqueueing-policy-fn]
  (let [available-capacity (nb-available-resources resource)
        {:keys [resource unqueued]}
        (sim-rc-queue/unqueue-event resource available-capacity unqueueing-policy-fn)]
    {:resource resource
     :events unqueued}))

(defn dispose-consumption-uuid
  "Dispose a specific `consumption-uuid` in the `resource`.

  `unqueueing-policy-fn` is used to determine in which order events should be released.

  Returns a map with:
  * `events` list of events to add in the `future-events` again.
  * `resource` updated with consumption `consumption-uuid` removed.
  * `errors` if freeing has raised an error"
  [resource unqueueing-policy-fn consumption-uuid]
  (let [{:keys [resource errors]} (sim-rc-consumption/ended resource consumption-uuid)]
    (if errors
      {:resource resource
       :errors errors}
      (unqueue-attempts resource unqueueing-policy-fn))))

(defn update-capacity
  "Returns a map with
  * `events` list of events that should be seized again
  * `resource` updated with the resource capacity
  * `preempts` the list of event to stop execution of

  If the new capacity is lower than the number of element consumed (i.e. in `consumption`), then the `preemption-policy` choose one event to stop:
  * `::no-premption` is the only implemented, it doesn't do anything and let the currently executing event finish."
  [resource preemption-policy-fn unqueueing-policy-fn new-capacity]
  (let [{::sim-engine/keys [capacity]
         :or {capacity 1}}
        resource
        resource (assoc resource ::sim-engine/capacity new-capacity)]
    (if (< new-capacity capacity)
      ;;NOTE Capacity decrease
      {:preempts (preemption-policy-fn resource)}
      ;;NOTE Capacity increase
      (unqueue-attempts resource unqueueing-policy-fn))))

(defn dispose
  "Dispose `quantity` items of `resource`

  `unqueueing-policy-fn` is used to determine in which order events should be released.

  Returns a map with:
  * `events` list of events that should be try to seize again the resource.
  * `resource` updated with consumptions removed.
  * `errors` if consumption-uuid found are not existing"
  [resource priority-comp unqueueing-policy-fn quantity]
  (let [capacity (::sim-engine/capacity resource)]
    (if-not (and (integer? capacity) (pos? capacity))
      {:errors [#::sim-engine{:why :resource-dont-have-capacity
                              :capacity (::sim-engine/capacity resource)}]}
      (let [consumptions (sim-rc-consumption/consumption-by-priority resource priority-comp)]
        (loop [[[consumption-uuid consumption] & rconsumptions] consumptions
               quantity-to-dispose quantity
               resource resource
               all-errors []]
          (cond
            (= 0 quantity-to-dispose)
            ;;NOTE The quantity to dispose is achieved
            (cond-> (unqueue-attempts resource unqueueing-policy-fn)
              (seq all-errors) (assoc :errors all-errors))
            (and (or (nil? consumption-uuid) (empty? consumption)) (> quantity-to-dispose 0))
            ;;NOTE No more consumption but the disposed quantity is not reach yet
            {:errors [#::sim-engine{:why :cant-dispose-quantity
                                    :capacity capacity
                                    :consumption-uuid consumption-uuid
                                    :quantity quantity
                                    :quantity-to-dispose quantity-to-dispose}]}
            :else (let [{::sim-engine/keys [consumption-quantity]} consumption]
                    (if-not (and (integer? consumption-quantity) (pos? consumption-quantity))
                      ;;NOTE Consumption is malformed
                      {:errors [#::sim-engine{:why :consumption-malformed
                                              :consumption-quantity consumption-quantity}]}
                      (if (>= quantity-to-dispose consumption-quantity)
                        ;;NOTE Even after that consumption removed, there are still to dispose
                        (let [{:keys [resource errors]} (sim-rc-consumption/ended resource
                                                                                  consumption-uuid)]
                          (recur rconsumptions
                                 (- quantity-to-dispose consumption-quantity)
                                 resource
                                 (reduce conj all-errors errors)))
                        ;;NOTE Just a part of this consumption should be removed
                        (let [resource (update-in resource
                                                  [::sim-engine/consumption consumption-uuid]
                                                  update
                                                  ::sim-engine/consumption-quantity
                                                  -
                                                  quantity-to-dispose)]
                          (cond-> (unqueue-attempts resource unqueueing-policy-fn)
                            (seq all-errors) (assoc :errors all-errors))))))))))))
