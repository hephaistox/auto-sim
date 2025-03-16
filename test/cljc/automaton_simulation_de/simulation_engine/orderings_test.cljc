(ns auto-sim.simulation-engine.orderings-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.simulation-engine.orderings :as sut]
   [automaton-core.adapters.schema       :as core-schema]))

(deftest schema-test (is (= nil (core-schema/validate-humanize sut/schema))))
