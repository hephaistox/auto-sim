(ns auto-sim.simulation-engine.impl.stopping-definition.state-contains
  (:require
   [auto-sim.simulation-engine :as-alias sim-engine]))

(defn stop?
  "Is the `snapshot` state contains value under params `state`. Considers empty collection as no value"
  [snapshot
   {:keys [state]
    :as _params}]
  (let [snapshot-state (get snapshot ::sim-engine/state {})
        state-entry (get-in snapshot-state state)]
    #:auto-sim.simulation-engine{:stop? (if (coll? state-entry)
                                                         (not-empty state-entry)
                                                         (some? state-entry))
                                                :context
                                                #:auto-sim.simulation-engine{:snapshot-state
                                                                                            snapshot-state}}))

(defn stopping-definition
  []
  #:auto-sim.simulation-engine{:doc "Stops when `state` path is containing any value"
                                              :id :state-contains
                                              :next-possible? true
                                              :stopping-evaluation stop?})
