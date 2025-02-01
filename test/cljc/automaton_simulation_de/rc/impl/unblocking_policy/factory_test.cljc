(ns auto-sim.rc.impl.unblocking-policy.factory-test
  (:require
   [auto-sim.rc.impl.unblocking-policy.factory :as sut]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])))

(deftest factory-test (testing "nil policy is defaulted" (is (sut/factory {} nil))))
