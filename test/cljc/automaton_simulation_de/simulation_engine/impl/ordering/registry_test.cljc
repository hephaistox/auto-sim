(ns auto-sim.simulation-engine.impl.ordering.registry-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [auto-sim.simulation-engine.impl.ordering.registry :as sut]
   [automaton-core.adapters.schema                    :as core-schema]))

(deftest schema-test (is (= nil (core-schema/validate-humanize sut/schema))))

(deftest registry-test
  (testing "Test built-in registry compliance to schema."
    (is (= nil (core-schema/validate-data-humanize sut/schema (sut/build))))))
