(ns auto-sim.rc.impl.preemption-policy.registry-test
  (:require
   [auto-sim.rc.impl.preemption-policy.registry :as sut]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema              :as core-schema]))

(deftest registry-test
  (testing "Validate registry"
    (is (nil? (core-schema/validate-data-humanize (sut/schema) (sut/registry))))))
