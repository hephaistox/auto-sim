(ns auto-sim.rc.unblocking-policy-test
  (:require
   [automaton-core.adapters.schema               :as core-schema]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [auto-sim.rc.unblocking-policy :as sut]))

(deftest schema-test
  (testing "Validate schema" (is (nil? (core-schema/validate-humanize (sut/schema))))))
