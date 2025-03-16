(ns auto-sim.event-registry-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [auto-sim.simulation-engine.impl.event-registry :as sut]
   [automaton-core.adapters.schema                 :as core-schema]))

(deftest schema-test
  (testing "event registry has a valid schema"
    (is (nil? (core-schema/validate-humanize sut/schema)))))
