(ns auto-sim.rc.queue
  "The queue of a resource is containing all events blocked while waiting for an available resource."
  (:require
   [auto-sim.rc :as-alias sim-rc]))

(defn queue-event
  "Adds `event` in the queue of `resource`.

  Elements in the `queue` are stored under the `:auto-sim.rc/queue` key and are defined with:
  * `:auto-sim.rc/seizing-event` the `event`
  * `:auto-sim.rc/consumed-quantity` = `consumed-quantity` (should be a strictly positive integer, otherwise queueuing is ignored).

  Queueing is skipped if `consumed-quantity` is not a valid positive integer "
  [resource consumed-quantity event]
  (cond-> (or resource {})
    (and (integer? consumed-quantity) (pos-int? consumed-quantity) (seq event))
    (update ::sim-rc/queue
            (fnil #(conj %
                         #::sim-rc{:seizing-event event
                                   :consumed-quantity consumed-quantity})
                  []))))

(defn unqueue-event
  "Removes events in the `queue` of `resource`. The `events` are removed in the order defined by `unblocking-policy-fn`, they are removed while `available-quantity` is not fully consumed.

  Returns a pair of:
  * the unqueued events,
  * the updated `resource` without this event waiting anymore.

  Note that it may happen that the availability changes before that event is actually executed, (i.e. a higher priority event change the number of available resources...)."
  [{::sim-rc/keys [queue]
    :as resource}
   available-capacity
   unblocking-policy-fn]
  (if (<= available-capacity 0)
    [[] resource]
    (loop [unblocked-events []
           queue queue
           released-capacity 0]
      (let [[blocking new-queue] (unblocking-policy-fn queue)
            blocked-quantity (get blocking ::sim-rc/consumed-quantity 1)
            new-released-quantity (+ released-capacity blocked-quantity)]
        (cond
          (nil? blocking) [unblocked-events
                           (assoc resource ::sim-rc/queue (if (nil? new-queue) [] new-queue))]
          (>= available-capacity new-released-quantity)
          (recur (conj unblocked-events blocking) new-queue new-released-quantity)
          :else [unblocked-events (assoc resource ::sim-rc/queue (if (nil? queue) [] queue))])))))
