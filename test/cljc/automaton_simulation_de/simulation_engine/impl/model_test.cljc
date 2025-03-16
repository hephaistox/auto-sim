(ns auto-sim.simulation-engine.impl.model-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.simulation-engine               :as-alias sim-engine]
   [auto-sim.simulation-engine.impl.model    :as sut]
   [auto-sim.simulation-engine.impl.registry :as sim-de-registry]
   [automaton-core.adapters.schema           :as core-schema]))

(deftest schema-test (is (= nil (core-schema/validate-humanize sut/schema))))

(deftest build-test
  (is (nil? (->> (sut/build #:auto-sim.simulation-engine{} (sim-de-registry/build))
                 (core-schema/validate-data-humanize sut/schema)))
      "Minimal model with registry only is valid.")
  (is (nil? (->> (sut/build #:auto-sim.simulation-engine{} (sim-de-registry/build))
                 (core-schema/validate-data-humanize sut/schema)))
      "Default registry is valid.")
  (is (->> (sut/build {} [])
           (core-schema/validate-data-humanize sut/schema)
           :error)
      "model-data can't be an empty map, building result is not validated."))
