(ns auto-sim.simulation-engine.impl.built-in-sd.execution-not-found-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.simulation-engine                                      :as-alias sim-engine]
   [auto-sim.simulation-engine.impl.built-in-sd.execution-not-found :as sut]
   [auto-sim.simulation-engine.impl.stopping.definition             :as sim-de-sc-definition]
   [auto-sim.simulation-engine.response                             :as sim-de-response]
   [automaton-core.adapters.schema                                  :as core-schema]))

(deftest stopping-definition-test
  (is (= nil
         (->> sut/stopping-definition
              (core-schema/validate-data-humanize sim-de-sc-definition/schema)))))

(deftest evaluates-test
  (is (= nil
         (->> (sut/evaluates
               #:auto-sim.simulation-engine{:stopping-causes []
                                            :snapshot #:auto-sim.simulation-engine{:id 1
                                                                                   :iteration 1
                                                                                   :date 1
                                                                                   :state {}
                                                                                   :past-events []
                                                                                   :future-events
                                                                                   []}}
               #:auto-sim.simulation-engine{:type :a
                                            :date 12})
              (core-schema/validate-data-humanize sim-de-response/schema)))))
