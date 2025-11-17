(ns auto-sim.links
  (:require
   [auto-sim :as-alias sim]))

(def links
  (->> [{:url "simulation/machine.svg"
         :id ::sim/machine}
        {:url "simulation/product.svg"
         :id ::sim/product}
        {:url "simulation/source.svg"
         :id ::sim/source}
        {:url "simulation/sink.svg"
         :id ::sim/sink}]
       (mapv (fn [link] [(:id link) link]))
       (into {})))
