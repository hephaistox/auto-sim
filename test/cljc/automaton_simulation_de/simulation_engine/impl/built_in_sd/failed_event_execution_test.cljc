(ns auto-sim.simulation-engine.impl.built-in-sd.failed-event-execution-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.simulation-engine                                         :as-alias sim-engine]
   [auto-sim.simulation-engine.impl.built-in-sd.failed-event-execution :as sut]
   [auto-sim.simulation-engine.impl.stopping.definition                :as sim-de-sc-definition]
   [auto-sim.simulation-engine.response                                :as sim-de-response]
   [automaton-core.adapters.schema                                     :as core-schema]))

(deftest stopping-definition-test
  (is (= nil
         (->> sut/stopping-definition
              (core-schema/validate-data-humanize sim-de-sc-definition/schema)))))

(deftest stopping-cause-test
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
               nil
               #:auto-sim.simulation-engine{:type :a
                                            :date 1})
              (core-schema/validate-data-humanize sim-de-response/schema)))))
