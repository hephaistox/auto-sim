(ns auto-sim.engine-test
  (:require
   [auto-sim.engine   :as sut]
   [auto-sim.ordering :as sim-ordering]
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])))

;; ********************************************************************************
;;  Helpers
;; ********************************************************************************

(def order-stub (sim-ordering/sorter (sim-ordering/fields :foo) (sim-ordering/types [:a :b :c])))

(deftest initial-snapshot-test
  (is (= #::sut{:id 1
                :iteration 1
                :bucket 10
                :state {}
                :past-events []
                :future-events []}
         (sut/initial-snapshot 10 nil nil))
      "Empty state and future-events")
  (is (= #::sut{:id 1
                :iteration 1
                :bucket 0
                :state {:foo :bar}
                :past-events []
                :future-events [{:a :b}]}
         (sut/initial-snapshot 0 {:foo :bar} [{:a :b}]))
      "State and future events are copied"))

(deftest continue-test
  (is (= #::sut{:stopping-criteria [#::sut{:stopping-criteria ::sut/missing-sorter}]
                :id 1
                :iteration 1
                :bucket 0
                :state {}
                :past-events []
                :future-events []}
         (->> (sut/initial-snapshot 0 {} nil)
              (sut/continue {})))
      "A missing sorter is a stopping criteria")
  (is
   (= #::sut{:stopping-criteria [#::sut{:stopping-criteria ::sut/no-future-events}]
             :id 1
             :iteration 1
             :bucket 0
             :state {}
             :past-events []
             :future-events []}
      (->> (sut/initial-snapshot 0 {} nil)
           (sut/continue #::sut{:sorter order-stub})))
   "When there are no future events, the stopping criteria is raised, no modification is made on the snapshot")
  (is
   (= #::sut{:stopping-criteria [#::sut{:stopping-criteria ::sut/execution-not-found
                                        :not-found-type :a
                                        :possible-types []}]
             :id 1
             :iteration 1
             :bucket 0
             :state {}
             :past-events [#::sut{:type :a}]
             :future-events []}
      (->> [#::sut{:type :a}]
           (sut/initial-snapshot 0 {})
           (sut/continue #::sut{:sorter order-stub})))
   "When no event execution is found, the stopping criteria is raised, no modification is made on the snapshot")
  (is
   (= #::sut{:id 2
             :iteration 2
             :bucket 2
             :state {:foo 1}
             :past-events [#::sut{:type :a
                                  :bucket 2}]
             :future-events []
             :stopping-criteria [#::sut{:stopping-criteria ::sut/no-future-events}]}
      (->> [#::sut{:type :a
                   :bucket 2}]
           (sut/initial-snapshot 0 {})
           (sut/continue #::sut{:sorter order-stub
                                :event-registry {:a (fn [_ _current-bucket state future-events]
                                                      #::sut{:future-events future-events
                                                             :state
                                                             (update state :foo (fnil inc 0))})}})))
   "A simple event execution advances id and iteration, date is going to the event bucket, state is modified. Execution stops with no future events as no more event are existing.")
  (is
   (= #::sut{:id 3
             :iteration 3
             :bucket 5
             :state {:foo 2}
             :past-events [#::sut{:type :a
                                  :bucket 2}
                           #::sut{:type :a
                                  :bucket 5}]
             :future-events []
             :stopping-criteria [#::sut{:stopping-criteria ::sut/no-future-events}]}
      (->> [#::sut{:type :a
                   :bucket 2}
            #::sut{:type :a
                   :bucket 5}]
           (sut/initial-snapshot 0 {})
           (sut/continue #::sut{:sorter order-stub
                                :event-registry {:a (fn [_ _ state future-events]
                                                      #::sut{:future-events future-events
                                                             :state
                                                             (update state :foo (fnil inc 0))})}})))
   "A doubled execution of the same event advances twice id and iteration. State is modified twice also. Past events are stored in the execution order.")
  (is
   (= #::sut{:id 3
             :iteration 3
             :bucket 10
             :state {:foo 2
                     :bar 1}
             :past-events [#::sut{:type :b
                                  :bucket 2}
                           #::sut{:type :a
                                  :bucket 10}]
             :future-events []
             :stopping-criteria [#::sut{:stopping-criteria ::sut/no-future-events}]}
      (->> [#::sut{:type :b
                   :bucket 2}]
           (sut/initial-snapshot 0 {})
           (sut/continue #::sut{:sorter order-stub
                                :event-registry
                                {:a (fn [_ _ state future-events]
                                      #::sut{:future-events future-events
                                             :state (update state :foo (comp inc (fnil inc 0)))})
                                 :b (fn [_ _ state future-events]
                                      #::sut{:future-events (conj future-events
                                                                  #::sut{:type :a
                                                                         :bucket 10})
                                             :state (update state :bar (fnil inc 0))})}})))
   "An event creating an event is working. Each state update is executed. Pas-events are stored in the execution order.")
  (is
   (= #::sut{:id 1
             :iteration 1
             :bucket 10
             :state {}
             :past-events [#::sut{:type :a
                                  :bucket 2}]
             :future-events []
             :stopping-criteria [#::sut{:stopping-criteria ::sut/causality-broken
                                        :current-bucket 10
                                        :event-bucket 2}]}
      (->> [#::sut{:type :a
                   :bucket 2}]
           (sut/initial-snapshot 10 {})
           (sut/continue #::sut{:sorter order-stub
                                :event-registry {:a (fn [_ _ state future-events]
                                                      #::sut{:future-events future-events
                                                             :state
                                                             (update state :foo (fnil inc 0))})}})))
   "When causality is broken, there is no advancement on `id` and `iteration`. The faulty event is moved to `past-events`")
  (is (= #::sut{:bucket 2
                :future-events []
                :id 2
                :iteration 2
                :state {}
                :past-events [#::sut{:type :a
                                     :bucket 2}]
                :stopping-criteria [#::sut{:stopping-criteria
                                           :auto-sim.engine/failed-event-execution
                                           :current-event #::sut{:type :a
                                                                 :bucket 2}
                                           :exception true}]}
         (update-in (->> [#::sut{:type :a
                                 :bucket 2}]
                         (sut/initial-snapshot 1 {})
                         (sut/continue #::sut{:sorter order-stub
                                              :event-registry {:a (fn [_ _ _ _]
                                                                    (throw (ex-info "test" {})))}}))
                    [::sut/stopping-criteria 0 ::sut/exception]
                    some?))
      "When execution is failing, returns a failed-event-execution"))
