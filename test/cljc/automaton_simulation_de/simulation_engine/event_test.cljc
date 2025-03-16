(ns auto-sim.simulation-engine.event-test
  (:require
   [auto-sim.simulation-engine       :as-alias sim-engine]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [auto-sim.simulation-engine.event :as sut]
   [automaton-core.adapters.schema   :as core-schema]))

(deftest schema-test
  (testing "Test schema of event" (is (= nil (core-schema/validate-humanize sut/schema))))
  (testing "Test valid events"
    (is (= nil
           (core-schema/validate-data-humanize sut/schema
                                               #:auto-sim.simulation-engine{:type :a
                                                                            :date 12})))))

(deftest postpone-events-test
  (testing "Empty events are ok" (is (empty? (sut/postpone-events nil nil nil))))
  (testing "Example"
    (is
     (= [#:auto-sim.simulation-engine{:type :a
                                      :date 1}
         #:auto-sim.simulation-engine{:type :b
                                      :date 666}
         #:auto-sim.simulation-engine{:type :a
                                      :date 3}
         #:auto-sim.simulation-engine{:type :c
                                      :date 666}]
        (sut/postpone-events [#:auto-sim.simulation-engine{:type :a
                                                           :date 1}
                              #:auto-sim.simulation-engine{:type :b
                                                           :date 2}
                              #:auto-sim.simulation-engine{:type :a
                                                           :date 3}
                              #:auto-sim.simulation-engine{:type :c
                                                           :date 10}]
                             (comp even? ::sim-engine/date)
                             666))))
  (testing "None updated"
    (is
     (= [#:auto-sim.simulation-engine{:type :a
                                      :date 1}
         #:auto-sim.simulation-engine{:type :b
                                      :date 5}
         #:auto-sim.simulation-engine{:type :a
                                      :date 3}
         #:auto-sim.simulation-engine{:type :c
                                      :date 11}]
        (sut/postpone-events [#:auto-sim.simulation-engine{:type :a
                                                           :date 1}
                              #:auto-sim.simulation-engine{:type :b
                                                           :date 5}
                              #:auto-sim.simulation-engine{:type :a
                                                           :date 3}
                              #:auto-sim.simulation-engine{:type :c
                                                           :date 11}]
                             (comp even? ::sim-engine/date)
                             666)))))
