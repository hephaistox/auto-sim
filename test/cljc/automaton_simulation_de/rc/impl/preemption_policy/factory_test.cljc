(ns auto-sim.rc.impl.preemption-policy.factory-test
  (:require
   [auto-sim.rc.impl.preemption-policy.factory :as sut]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])))

(deftest factory-test (testing "Defaulted" (is (some? (sut/factory {} nil)))))
