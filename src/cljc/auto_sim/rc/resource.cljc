(ns auto-sim.rc.resource
  "This namespace is assembling the consumption and queue logics."
  (:require
   [auto-sim.engine                  :as-alias sim-engine]
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
  (max 0 (- (or capacity 0) (nb-consumed-resources resource))))

(defn seize
  "If a `resource` contains enough available items, a `consumption` is created and stored in the `resource`.
  Otherwise, no `consumption` is created and the `event` is queued in the `resource`, to test the seizing later on.

  In any case, it returns a map with:
  * `consumption-uuid` or `nil` if the execution is postponed awaiting for a resource disposal
  *  updated `resource` reflecting the consumption
  * `errors`"
  [resource event consumption-quantity priority]
  (if (>= (compare (nb-available-resources resource) consumption-quantity) 0)
    (sim-rc-consumption/consume resource event consumption-quantity priority)
    (sim-rc-queue/queue-event resource event consumption-quantity priority)))

(defn new-available-items
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
  "Removes a specific `consumption-uuid` in the `resource`.

  `unqueueing-policy-fn` is used to determine in which order events should be released.

  Returns a map with:
  * `events` list of events to add in the `future-events` again.
  * `resource` updated with consumption `consumption-uuid` removed."
  [resource unqueueing-policy-fn consumption-uuid]
  (let [{:keys [resource errors]} (sim-rc-consumption/free resource consumption-uuid)]
    (if errors
      {:resource resource
       :errors errors}
      (new-available-items resource unqueueing-policy-fn))))

(defn update-capacity
  "Returns a map with
  * `unqueued` list of events that should be seized again
  * `resource` updated with the resource capacity
  * `preempts` the list of event to stop execution of

  If the new capacity is lower than the number of element consumed (i.e. in `consumption`), then the `preemption-policy` choose one event to stop:
  * `::no-premption` is the only implemented, it doesn't do anything and let the currently executing event finish."
  [{::sim-engine/keys [capacity]
    :or {capacity 1}
    :as resource}
   preemption-policy-fn
   unqueueing-policy-fn
   new-capacity]
  (let [resource (assoc resource ::sim-engine/capacity new-capacity)]
    (if (< new-capacity capacity)
      ;;NOTE Capacity decrease
      (preemption-policy-fn resource)
      ;;NOTE Capacity increase
      (new-available-items resource unqueueing-policy-fn))))

(defn dispose
  "Dispose `quantity` items of `resource` that has been seized by entity `entity-id`.

  `unqueueing-policy-fn` is used to determine in which order events should be released.

  Returns a map with:
  * `unqueued` list of events that should be try to seize again the resource.
  * `resource` updated with consumptions removed."
  [resource entity-id unqueueing-policy-fn priority-comp quantity]
  (let [consumptions (sim-rc-consumption/consumption-by-priority resource entity-id priority-comp)]
    (loop [consumptions consumptions
           quantity-to-dispose quantity
           resource resource
           all-errors []]
      (let [[[[consumption-uuid consumption]] & rconsumptions] consumptions
            {:keys [consumption-quantity]} consumption
            new-quantity-to-dispose (- quantity-to-dispose consumption-quantity)
            {:keys [resource errors]} (sim-rc-consumption/free resource consumption-uuid)]
        (if (pos? new-quantity-to-dispose)
          (recur rconsumptions new-quantity-to-dispose resource (reduce conj all-errors errors))
          (cond-> (new-available-items resource unqueueing-policy-fn)
            all-errors (assoc :errors all-errors)))))))
