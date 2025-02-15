(ns auto-sim.rc.unblocking-policy "The simple policies to unblock an event in the queue.")

(defn fifo
  "Select the first blocked consumer in the queue."
  [[next-unblocked-event & rest-blocked :as _queue]]
  [next-unblocked-event rest-blocked])

(defn lifo "Select the last blocked consumer in the queue." [queue] [(last queue) (butlast queue)])
