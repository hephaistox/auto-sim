(ns auto-sim.simulation-engine.snapshot-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [automaton-core.adapters.schema                     :as core-schema]
   [auto-sim.simulation-engine          :as-alias sim-engine]
   [auto-sim.simulation-engine.snapshot :as sut]))

(deftest schema-test
  (testing "Testing schema validity" (is (= nil (core-schema/validate-humanize sut/schema))))
  (testing "Test an example of iteration"
    (is (= nil
           (core-schema/validate-data-humanize
            sut/schema
            #:auto-sim.simulation-engine{:id 10
                                                        :iteration 10
                                                        :date 12
                                                        :state {}
                                                        :past-events []
                                                        :future-events []})))))

(deftest consume-first-event-test
  (testing "Nullable values"
    (is (= #:auto-sim.simulation-engine{:id 1
                                                       :iteration nil
                                                       :date nil
                                                       :state nil
                                                       :past-events []
                                                       :future-events []}
           (sut/consume-first-event nil))))
  (testing
    "As expected, current event is first future, new future head is dropped, snapshot and id, date is the next current date"
    (is
     (=
      #:auto-sim.simulation-engine{:id 2
                                                  :iteration 2
                                                  :date 7
                                                  :state {:foo :bar}
                                                  :past-events
                                                  [#:auto-sim.simulation-engine{:type
                                                                                               :a
                                                                                               :date
                                                                                               1}
                                                   #:auto-sim.simulation-engine{:type
                                                                                               :c
                                                                                               :date
                                                                                               7}]
                                                  :future-events
                                                  [#:auto-sim.simulation-engine{:type
                                                                                               :b
                                                                                               :date
                                                                                               12}]}
      (sut/consume-first-event
       #:auto-sim.simulation-engine{:id 1
                                                   :iteration 2
                                                   :date 3
                                                   :state {:foo :bar}
                                                   :past-events
                                                   [#:auto-sim.simulation-engine{:type
                                                                                                :a
                                                                                                :date
                                                                                                1}]
                                                   :future-events
                                                   [#:auto-sim.simulation-engine{:type
                                                                                                :c
                                                                                                :date
                                                                                                7}
                                                    #:auto-sim.simulation-engine{:type
                                                                                                :b
                                                                                                :date
                                                                                                12}]}))))
  (testing "nil current-event is ok"
    (is
     (=
      #:auto-sim.simulation-engine{:id 2
                                                  :iteration 2
                                                  :date 3
                                                  :state {:foo :bar}
                                                  :past-events
                                                  [#:auto-sim.simulation-engine{:type
                                                                                               :a
                                                                                               :date
                                                                                               1}]
                                                  :future-events []}
      (sut/consume-first-event
       #:auto-sim.simulation-engine{:id 1
                                                   :iteration 2
                                                   :date 3
                                                   :state {:foo :bar}
                                                   :past-events
                                                   [#:auto-sim.simulation-engine{:type
                                                                                                :a
                                                                                                :date
                                                                                                1}]
                                                   :future-events nil}))))
  (testing "Useless data in snapshot are forgot"
    (is
     (=
      #:auto-sim.simulation-engine{:id 2
                                                  :iteration 2
                                                  :date 7
                                                  :state {:foo :bar}
                                                  :past-events
                                                  [#:auto-sim.simulation-engine{:type
                                                                                               :a
                                                                                               :date
                                                                                               1}
                                                   #:auto-sim.simulation-engine{:type
                                                                                               :c
                                                                                               :date
                                                                                               7}]
                                                  :future-events
                                                  [#:auto-sim.simulation-engine{:type
                                                                                               :b
                                                                                               :date
                                                                                               12}]}
      (->
        #:auto-sim.simulation-engine{:id 1
                                                    :iteration 2
                                                    :date 3
                                                    :state {:foo :bar}
                                                    :past-events
                                                    [#:auto-sim.simulation-engine{:type
                                                                                                 :a
                                                                                                 :date
                                                                                                 1}]
                                                    :future-events
                                                    [#:auto-sim.simulation-engine{:type
                                                                                                 :c
                                                                                                 :date
                                                                                                 7}
                                                     #:auto-sim.simulation-engine{:type
                                                                                                 :b
                                                                                                 :date
                                                                                                 12}]}
        (assoc :will :disappear)
        sut/consume-first-event)))))

(deftest inconsistency?-test
  (testing "Empty past and future are consistent"
    (is (false? (sut/inconsistency? #:auto-sim.simulation-engine{:date 2
                                                                                :future-events []
                                                                                :past-events []}))))
  (testing "Past events before 5, snapshot at 5, and future after 5 are seen as consistent"
    (is
     (false?
      (sut/inconsistency?
       #:auto-sim.simulation-engine{:date 5
                                                   :future-events
                                                   [#:auto-sim.simulation-engine{:type
                                                                                                :b
                                                                                                :date
                                                                                                20}
                                                    #:auto-sim.simulation-engine{:type
                                                                                                :a
                                                                                                :date
                                                                                                7}
                                                    #:auto-sim.simulation-engine{:type
                                                                                                :b
                                                                                                :date
                                                                                                5}]
                                                   :past-events
                                                   [#:auto-sim.simulation-engine{:type
                                                                                                :b
                                                                                                :date
                                                                                                4}
                                                    #:auto-sim.simulation-engine{:type
                                                                                                :a
                                                                                                :date
                                                                                                5}]}))))
  (testing "Too early future events are returned as inconsistency"
    (is
     (=
      {:snapshot-date 2
       :mismatching-events
       #:auto-sim.simulation-engine{:future-events
                                                   [#:auto-sim.simulation-engine{:type
                                                                                                :a
                                                                                                :date
                                                                                                1}]
                                                   :past-events []}}
      (sut/inconsistency?
       #:auto-sim.simulation-engine{:date 2
                                                   :future-events
                                                   [#:auto-sim.simulation-engine{:type
                                                                                                :b
                                                                                                :date
                                                                                                20}
                                                    #:auto-sim.simulation-engine{:type
                                                                                                :a
                                                                                                :date
                                                                                                1}]
                                                   ::sut/past-events []}))))
  (testing "Too late past events are returned as inconsistency"
    (is
     (=
      {:snapshot-date 2
       :mismatching-events
       #:auto-sim.simulation-engine{:past-events
                                                   [#:auto-sim.simulation-engine{:type
                                                                                                :b
                                                                                                :date
                                                                                                20}]
                                                   :future-events []}}
      (sut/inconsistency?
       #:auto-sim.simulation-engine{:date 2
                                                   :future-events []
                                                   :past-events
                                                   [#:auto-sim.simulation-engine{:type
                                                                                                :b
                                                                                                :date
                                                                                                20}
                                                    #:auto-sim.simulation-engine{:type
                                                                                                :a
                                                                                                :date
                                                                                                1}]})))))
