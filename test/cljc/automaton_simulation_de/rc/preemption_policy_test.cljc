(ns auto-sim.rc.preemption-policy-test
  (:require
   [auto-sim.rc.preemption-policy  :as sut]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema :as core-schema]))

(deftest schema-test
  (testing "Schema is valid" (is (nil? (core-schema/validate-humanize (sut/schema))))))
