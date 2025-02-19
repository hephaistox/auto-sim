(ns auto-sim.machine
  "Model machine.

  Machine expects `::sim-engine/current-operation` field to be set."
  (:require
   [auto-sim.engine :as-alias sim-engine]
   [auto-sim.route  :as sim-route]))

(defn infinite-capacity
  "Models a machine processing during.

  The duration of the processing is taken in the entity state [`::sim-engine/current-operation` `kw`].

  Returns `event-return` updated with `postponed-event`,  at `(+ pt bucket)`"
  [event-return event bucket postponed-event kw]
  (let [current-operation (sim-route/current-operation event-return event bucket)]
    (if (nil? current-operation)
      (update event-return
              ::sim-engine/errors
              (fnil conj [])
              #::sim-engine{:why :current-operation-is-nil})
      (let [pt (get current-operation kw)]
        (if (and (number? bucket) (number? pt) (pos? pt))
          (sim-route/schedule event-return event (+ bucket pt) postponed-event)
          (update event-return
                  ::sim-engine/errors
                  (fnil conj [])
                  {::sim-engine/why :buckets-cant-be-calculated
                   :machine-proceessing pt
                   :kind kw
                   :bucket bucket}))))))
