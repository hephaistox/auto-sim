(ns auto-sim.simulation-engine.impl.built-in-sd.no-future-events
  "`stopping-definition` to stop when no future events exists anymore."
  (:require
   [auto-sim.simulation-engine         :as-alias sim-engine]
   [auto-sim.simulation-engine.request :as sim-de-request]))

(def stopping-definition
  #:auto-sim.simulation-engine{:id ::sim-engine/no-future-events
                                              :next-possible? false
                                              :doc "Stops when no future events exists anymore."})

(defn evaluates
  [request future-events]
  (cond-> request
    (empty? future-events)
    (sim-de-request/add-stopping-cause
     #:auto-sim.simulation-engine{:stopping-criteria
                                                 #:auto-sim.simulation-engine{:stopping-definition
                                                                                             stopping-definition}})))
