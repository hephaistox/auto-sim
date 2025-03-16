(ns auto-sim.simulation-engine.request-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [auto-sim.simulation-engine         :as-alias sim-engine]
   [auto-sim.simulation-engine.request :as sut]
   [automaton-core.adapters.schema     :as core-schema]))

(deftest schema-test
  (testing "Test the schema" (is (= nil (core-schema/validate-humanize sut/schema)))))

(deftest add-stopping-cause-test
  (is (= #:auto-sim.simulation-engine{:request true
                                      :stopping-causes [#:auto-sim.simulation-engine{:stopping-cause
                                                                                     true}]}
         (sut/add-stopping-cause #:auto-sim.simulation-engine{:request true}
                                 #:auto-sim.simulation-engine{:stopping-cause true}))
      "Adding a `stopping-cause` returns the request.")
  (is (= {:request true} (sut/add-stopping-cause {:request true} nil))
      "Adding a `stopping-cause` returns the request."))
