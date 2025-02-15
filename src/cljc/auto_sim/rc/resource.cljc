(ns auto-sim.rc.resource
  "A resource is a limited quantity of items that are used by entities as they proceed through the system. A resource has a capacity that governs the total quantity of items that may be available. All the items in the resource are homogeneous, meaning that they are indistinguishable. If an entity attempts to seize a resource that does not have any units available it must wait in a queue.
  It is often representing real world items that availability is limited (e.g. machine, wrench, ...).

  A resource knows its instantenous capacity and the element waiting for its availablility in the queue. The properties of a resource are:
  * `capacity` (default 1) total number of available resources, note that this number may evolve over time.
  * `currently-consuming` (default []) list of events currently consuming this resource, is useful to track them and enabling preemption and failures. The event contains the information on the waiting entity.
  * `queue` (default []) list of blocked entities.
  * `renewable?` (default true) when true, the disposing is not giving back the values."
  (:require
   [auto-sim.rc             :as-alias sim-rc]
   [auto-sim.rc.consumption :as sim-rc-consumption]
   [auto-sim.rc.queue       :as sim-rc-queue]))

(defn defaulting-values
  "Returns a resource with default values added."
  [{::sim-rc/keys [capacity currently-consuming queue renewable?]
    :as resource
    :or {capacity 1
         currently-consuming {}
         queue []
         renewable? true}}]
  (assoc resource
         ::sim-rc/capacity capacity
         ::sim-rc/currently-consuming currently-consuming
         ::sim-rc/queue queue
         ::sim-rc/renewable? renewable?))

(defn nb-consumed-resources
  "Returns the number of consumed resources."
  [{:keys [::sim-rc/currently-consuming]
    :as _resource}]
  (if (map? currently-consuming)
    (->> currently-consuming
         vals
         (map (fn [{::sim-rc/keys [consumed-quantity]
                    :or {consumed-quantity 1}}]
                consumed-quantity))
         (apply + 0))
    0))

(defn nb-available-resources
  "Returns the number of available resources based on the defined `capacity,` and the `currently-consuming` resources (i.e. sum of their )."
  [{::sim-rc/keys [capacity]
    :or {capacity 1}
    :as resource}]
  (max 0 (- (or capacity 0) (nb-consumed-resources resource))))

(defn seize
  "If a `resource` contains enough available resources, a consumption is created and stored in the `resource`.
  Otherwise, no consumption is created and the resource is queueing `event` to test the seizing later on.

  In any case, it returns a pair of:
  * `consumption-uuid` or `nil` if the execution is postponed awaiting for a resource disposal
  *  updated `resource` reflecting the consumption"
  [resource consumed-quantity event]
  (if (>= (compare (nb-available-resources resource) consumed-quantity) 0)
    (sim-rc-consumption/consume resource consumed-quantity event)
    [nil (sim-rc-queue/queue-event resource consumed-quantity event)]))

(defn dispose
  "Removes in the `resource` the `consumption-uuid`.
  The released quantity of resource may generate some `unblockings`

  Returns a pair with:
  * `unblockings` list of blockings that should be try to seize again the resource
  * `resource` updated with consumption `consumption-uuid` removed.

  `unblocking-policy-fn` is used to determine in which order events should be released."
  [resource consumption-uuid unblocking-policy-fn]
  (let [resource (sim-rc-consumption/free resource consumption-uuid)]
    (sim-rc-queue/unqueue-event resource (nb-available-resources resource) unblocking-policy-fn)))

(defn update-capacity
  "Returns a pair of:
  * `unblocked-events` list of events that should be try to seize again the
  * `resource` updated with the resource capacity

  If the new capacity is lower than the number of element consumed (i.e. in `currently-consuming`), then the `preemption-policy` choose one event to stop:
  * `::no-premption` is the only implemented, it doesn't do anything and let the currently executing event finish."
  [{::sim-rc/keys [capacity]
    :or {capacity 1}
    :as resource}
   new-capacity
   preemption-policy-fn
   unblocking-policy-fn]
  (let [resource (assoc resource ::sim-rc/capacity new-capacity)]
    (if (< new-capacity capacity)
      (preemption-policy-fn resource)
      ;; Capacity increase
      (sim-rc-queue/unqueue-event resource
                                  (nb-available-resources resource)
                                  unblocking-policy-fn))))
