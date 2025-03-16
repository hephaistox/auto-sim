(ns auto-sim.rc.impl.unblocking-policy.registry-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [auto-sim.rc.impl.unblocking-policy.registry :as sut]
   [automaton-core.adapters.schema              :as core-schema]))

(deftest schema-test
  (testing "Validate schema" (is (nil? (core-schema/validate-humanize (sut/schema))))))

(deftest registry-test
  (testing "Validate registry"
    (is (nil? (core-schema/validate-data-humanize (sut/schema) (sut/registry))))))
