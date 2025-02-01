(ns auto-sim.simulation-engine.impl.ordering.registry
  "Registry for ordering."
  (:require
   [auto-sim.simulation-engine          :as-alias sim-engine]
   [auto-sim.simulation-engine.ordering :as sim-de-ordering]))

(def schema [:map])

(defn build
  []
  #:auto-sim.simulation-engine{:compare-field {:comparison-fn
                                                              sim-de-ordering/compare-field}
                                              :compare-types {:comparison-fn
                                                              sim-de-ordering/compare-types}})
