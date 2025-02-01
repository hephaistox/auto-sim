(ns auto-sim.simulation-engine.impl.built-in-sd.request-validation
  "Stops when the request is not valid."
  (:require
   [auto-sim.simulation-engine :as-alias sim-engine]))

(defn stopping-definition
  []
  {::sim-engine/doc "Stops when the request is not valid."
   ::sim-engine/id ::sim-engine/request-schema
   ::sim-engine/next-possible? true})
