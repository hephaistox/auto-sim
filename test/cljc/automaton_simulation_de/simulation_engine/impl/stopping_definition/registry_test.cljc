(ns auto-sim.simulation-engine.impl.stopping-definition.registry-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [auto-sim.simulation-engine.impl.stopping-definition.registry :as sut]
   [automaton-core.adapters.schema                               :as core-schema]))

(deftest schema-test (is (= nil (core-schema/validate-humanize sut/schema))))

(deftest build-test
  (testing "Default built-in registry is valid."
    (is (= nil (core-schema/validate-data-humanize sut/schema (sut/build))))))
