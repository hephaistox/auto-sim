(ns auto-sim.machine
  "Model machines."
  (:require
   [auto-sim.engine :as-alias sim-engine]
   [auto-sim.entity :as sim-entity]))

(defn- infinite-capacity*
  [event-return event bucket pt new-event]
  (if (and (number? bucket) (number? pt) (pos? pt))
    (sim-entity/schedule-same-entity event-return event (+ bucket pt) new-event)
    (update event-return
            ::sim-engine/errors
            conj
            #::sim-engine{:why :buckets-cant-be-calculated
                          :pt pt
                          :bucket bucket})))

(defn infinite-capacity
  "Mimics a machine processing during `pt` buckets, an execute `event` then.

  `pt` is the value associated to key `kw` in the `current-operation` of `event`.

  Returns `event-return` updated with this new event, starting at `(+ pt bucket)`"
  [event-return event bucket postponed-event kw]
  (let [{::sim-engine/keys [current-operation]} event
        pt (get current-operation kw)
        new-event (merge event postponed-event)]
    (if (nil? current-operation)
      (update event-return
              ::sim-engine/errors
              conj
              #::sim-engine{:why :current-operation-should-be-set
                            :current-operation current-operation
                            :bucket bucket
                            :event event})
      (infinite-capacity* event-return event bucket pt new-event))))
