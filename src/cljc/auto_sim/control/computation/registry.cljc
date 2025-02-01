(ns auto-sim.control.computation.registry
  "Contains all the default possible computation implementation to choose from
   For diagram see ![computation](archi/control/computation_registry.png)"
  (:require
   [auto-sim.control                         :as-alias sim-de-control]
   [auto-sim.control.computation.impl.chunk  :as sim-de-comp-chunk]
   [auto-sim.control.computation.impl.direct :as sim-de-comp-direct]))

(defn computation-registry
  []
  {:direct sim-de-comp-direct/make-direct-computation
   :chunk sim-de-comp-chunk/make-chunk-computation})
