(ns auto-sim.stopping-criteria
  (:refer-clojure :exclude [eval])
  (:require
   [auto-sim.engine :as-alias sim-engine]))

(defn stop-bucket
  "Stops when the the `stopping-bucket` is reached, or after."
  [{::sim-engine/keys [stopping-bucket]}]
  (fn [snapshot]
    (let [{snapshot-bucket ::sim-engine/bucket} snapshot]
      (when (or (nil? snapshot-bucket) (nil? stopping-bucket) (>= snapshot-bucket stopping-bucket))
        #::sim-engine{:doc "Stop when the snapshot bucket is at `stopping-bucket` or later on."
                      :id ::bucket-stopping
                      :context {:stopping-bucket stopping-bucket
                                :snapshot-bucket snapshot-bucket}}))))

(defn stop-now
  "Stops now."
  []
  (fn [_snapshot]
    #::sim-engine{:doc "Criteria to stop right now."
                  :id ::stop-now
                  :context nil}))

(defn stop-state-contains
  [{::sim-engine/keys [state-path]}]
  (fn [snapshot]
    (let [snapshot-state (get snapshot ::sim-engine/state {})]
      (when-let [state-entry (get-in snapshot-state state-path)]
        #::sim-engine{:doc "Stops when `state-path` path is containing any value"
                      :id ::state-contains
                      :context #::sim-engine{:snapshot-state snapshot-state
                                             :state-entry state-entry}}))))

(defn eval
  "Evaluates the `stopping-fns` on `snapshot`.

  Returns `nil` if `stopping-evaluation` is not defined.
  Returns a map with `stop?` and `context`."
  [stopping-fns snapshot]
  (reduce (fn [res stopping-fn]
            (if-let [v (stopping-fn snapshot)]
              (conj res v)
              res))
          []
          stopping-fns))
