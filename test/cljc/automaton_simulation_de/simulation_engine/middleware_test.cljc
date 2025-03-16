(ns auto-sim.simulation-engine.middleware-test
  (:require
   [auto-sim.simulation-engine.middleware :as sut]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema        :as core-schema]))

(deftest id-schema-test (is (= nil (core-schema/validate-humanize sut/id-schema))))

(deftest schema-test
  (testing "Valid schema?" (is (= nil (core-schema/validate-humanize sut/schema)))))
