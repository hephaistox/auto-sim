(ns auto-sim.simulation-engine.orderings
  "Sequence of `ordering`."
  (:require
   [auto-sim.simulation-engine.ordering :as sim-de-ordering]))

(def schema [:sequential sim-de-ordering/schema])

