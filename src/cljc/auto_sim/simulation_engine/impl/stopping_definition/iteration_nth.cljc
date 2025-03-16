(ns auto-sim.simulation-engine.impl.stopping-definition.iteration-nth
  "`stopping-definition` to stop at a given iteration."
  (:require
   [auto-sim.simulation-engine :as-alias sim-engine]))

(defn stop-nth
  "Is the `snapshot`'s `iteration` greater than or equal to `n`, the parameter in `params`."
  [snapshot
   {::sim-engine/keys [n]
    :as _params}]
  (let [snapshot-iteration (get snapshot ::sim-engine/iteration 0)]
    #:auto-sim.simulation-engine{:stop? (or (nil? n) (>= snapshot-iteration n))
                                 :context #:auto-sim.simulation-engine{:iteration snapshot-iteration
                                                                       :n n}}))

(defn stopping-definition
  []
  #:auto-sim.simulation-engine{:doc "Stops when the iteration `n` is reached."
                               :id ::sim-engine/iteration-nth
                               :next-possible? true
                               :stopping-evaluation stop-nth})
