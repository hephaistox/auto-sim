(ns auto-sim.rc.unqueueing-policy
  "The simple policies to unqueue an event waiting for available resources.")

(defn fifo
  "Select the first queued consumer in the queue."
  [[unqueued-event & rqueue :as _queue]]
  [unqueued-event (or rqueue [])])

(defn lifo "Select the last queued consumer" [queue] [(last queue) (butlast queue)])
