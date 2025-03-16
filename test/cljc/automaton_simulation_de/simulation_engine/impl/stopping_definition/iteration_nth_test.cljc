(ns auto-sim.simulation-engine.impl.stopping-definition.iteration-nth-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [auto-sim.simulation-engine                                        :as-alias sim-engine]
   [auto-sim.simulation-engine.impl.stopping-definition.iteration-nth :as sut]
   [auto-sim.simulation-engine.impl.stopping.definition               :as sim-de-sc-definition]
   [automaton-core.adapters.schema                                    :as core-schema]))

(deftest stop-nth-test
  (testing "Stops when the iteration number of the snpahost is greater or equal to the parameter."
    (is (sut/stop-nth #:auto-sim.simulation-engine{:iteration 12}
                      #:auto-sim.simulation-engine{:n 10}))
    (is (sut/stop-nth #:auto-sim.simulation-engine{:iteration 12}
                      #:auto-sim.simulation-engine{:n 12})))
  (testing
    "Doesn't stop when the iteration number of the snpahost is greater or equal to the parameter."
    (is (not (:stop? (sut/stop-nth #:auto-sim.simulation-engine{:iteration 2}
                                   #:auto-sim.simulation-engine{:n 10})))))
  (testing "Snapshot with no iteration number is default to 0"
    (is (not (:stop? (sut/stop-nth {} #:auto-sim.simulation-engine{:n 12}))))
    (is (:auto-sim.simulation-engine/stop? (sut/stop-nth {} #:auto-sim.simulation-engine{:n 0})))
    (testing "No parameter is defaulted to `-1`, stopping whatever is happening."
      (is (sut/stop-nth {} {})))))

(deftest stopping-definition-test
  (is (= nil
         (->> (sut/stopping-definition)
              (core-schema/validate-data-humanize sim-de-sc-definition/schema)))))
