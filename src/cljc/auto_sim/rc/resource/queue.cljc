(ns auto-sim.rc.resource.queue
  "The queue of a resource is containing all events waiting for an available resource."
  (:require
   [auto-sim.engine :as-alias sim-engine]))

(defn queue-event
  "Adds `event` in the queue of `resource` to wait for further available resources.

  Elements in the `queue` are stored under the `:auto-sim.engine/queue` key and are defined with:
  * `:auto-sim.engine/event` the `event`
  * `:auto-sim.engine/priority` priority used to define which element should be managed first.
  * `:auto-sim.engine/consumption-quantity` = `consumption-quantity` (should be a strictly positive integer, otherwise queueuing is skipped).

  Queueing is skipped if `consumption-quantity` is not a valid positive integer.

  Returns a map of
  * `:resource` updated
  * `:errors` a vector"
  [resource event consumption-quantity priority]
  (cond
    (empty? event) {:resource resource
                    :errors [#::sim-engine{:why :queuing-an-empty-event
                                           :resource resource
                                           :consumption-quantity consumption-quantity
                                           :priority priority
                                           :event event}]}
    (not (and (integer? consumption-quantity) (pos-int? consumption-quantity)))
    {:resource resource
     :errors [#::sim-engine{:why :consumption-quantity-wrong
                            :resource resource
                            :consumption-quantity consumption-quantity
                            :priority priority
                            :event event}]}
    :else {:resource (-> (or resource {})
                         (update ::sim-engine/queue
                                 (fnil #(conj %
                                              #::sim-engine{:event event
                                                            :priority priority
                                                            :consumption-quantity
                                                            consumption-quantity})
                                       [])))}))

(defn unqueue-event
  "Unqueue `events` from the `queue` of `resource`.

  The `events` are removed in the order defined by `unqueueing-policy-fn`, and while `available-quantity` is not fully consumed or all events unqueued.

  Returns a map of:
  * `unqueued` events,
  * and the updated `resource` without these events in the queue anymore.

  Note that unqueued events are not executed instantaneously, so event priority may lead to a situation where the availability changes before that event is actually executed."
  [resource available-capacity unqueueing-policy-fn]
  (let [{::sim-engine/keys [queue]} resource]
    (if (empty? queue)
      ;;NOTE No waiting event is in the queue
      {:resource (assoc resource ::sim-engine/queue [])}
      (loop [unqueued-events []
             queue queue
             released-capacity 0]
        (let [[unqueued new-queue] (unqueueing-policy-fn queue)
              unqueued-quantity (get unqueued ::sim-engine/consumption-quantity 1)
              new-released-quantity (+ released-capacity unqueued-quantity)]
          (cond
            (nil? unqueued)
            ;;NOTE All events have been unqueued
            {:unqueued unqueued-events
             :resource (assoc resource ::sim-engine/queue queue)}
            (> available-capacity new-released-quantity)
            ;;NOTE There are still available quantity, loop again
            (recur (conj unqueued-events unqueued) new-queue new-released-quantity)
            (= available-capacity new-released-quantity)
            ;;NOTE There is no more available quantity
            {:unqueued (conj unqueued-events unqueued)
             :resource (assoc resource ::sim-engine/queue new-queue)}
            :else
            ;;NOTE The last unqueued element has a too big released-quantity, stops before this unqueueing
            {:unqueued unqueued-events
             :resource (assoc resource ::sim-engine/queue queue)}))))))
