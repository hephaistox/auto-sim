(ns auto-sim.simulation-engine.impl.built-in-sd.response-validation
  "Stops when the response is not valid."
  (:require
   [auto-sim.simulation-engine :as-alias sim-engine]))

(defn stopping-definition
  []
  #:auto-sim.simulation-engine{:doc "Stops when the response is not valid."
                                              :id ::sim-engine/response-schema
                                              :next-possible? true})
