(ns auto-sim.engine-test
  (:require
   [auto-sim.engine   :as sut]
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.ordering :as sim-ordering]))

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
         (sut/initial-snapshot {} 10 nil nil))
      "Empty state and future-events")
  (is (= #::sut{:id 1
                :iteration 1
                :bucket 0
                :state {:foo :bar}
                :past-events []
                :future-events [{:a :b}]}
         (sut/initial-snapshot {} 0 {:foo :bar} [{:a :b}]))
      "State and future events are copied"))

(deftest continue-test
  (is (= #::sut{:stopping-criteria [#::sut{:id ::sut/missing-sorter
                                           :doc ["Event sorter is not defined"]}]
                :id 1
                :iteration 1
                :bucket 0
                :state {}
                :past-events []
                :future-events []}
         (-> {}
             (sut/initial-snapshot 0 {} nil)
             sut/continue))
      "A missing sorter is a stopping criteria")
  (is
   (= #::sut{:stopping-criteria [#::sut{:id ::sut/no-future-events
                                        :doc ["No more future events to execute"]}]
             :id 1
             :iteration 1
             :bucket 0
             :state {}
             :past-events []
             :sorter order-stub
             :future-events []}
      (-> #::sut{:sorter order-stub}
          (sut/initial-snapshot 0 {} nil)
          sut/continue))
   "When there are no future events, the stopping criteria is raised, no modification is made on the snapshot")
  (is
   (=
    #::sut{:stopping-criteria
           [#::sut{:id ::sut/execution-not-found
                   :doc
                   ["Event has an unknown event type `%s` (event is `%s`, possible values are `%s`)"
                    :a
                    #::sut{:type :a}
                    []]
                   :event #::sut{:type :a}
                   :not-found-type :a
                   :possible-types []}]
           :id 1
           :iteration 1
           :bucket 0
           :state {}
           :sorter order-stub
           :past-events [#::sut{:type :a}]
           :future-events []}
    (-> #::sut{:sorter order-stub}
        (sut/initial-snapshot 0 {} [#::sut{:type :a}])
        sut/continue))
   "When no event execution is found, the stopping criteria is raised, no modification is made on the snapshot")
  (is
   (= #::sut{:id 2
             :iteration 2
             :bucket 2
             :state {:foo 1}
             :past-events [#::sut{:type :a
                                  :bucket 2}]
             :future-events []
             :stopping-criteria [#::sut{:id ::sut/no-future-events
                                        :doc ["No more future events to execute"]}]}
      (select-keys (-> #::sut{:sorter order-stub
                              :event-registry {:a (fn [event-return _ _]
                                                    (-> event-return
                                                        (update-in [::sut/state :foo]
                                                                   (fnil inc 0))))}}
                       (sut/initial-snapshot 0
                                             {}
                                             [#::sut{:type :a
                                                     :bucket 2}])
                       sut/continue)
                   [::sut/id
                    ::sut/iteration
                    ::sut/bucket
                    ::sut/state
                    ::sut/past-events
                    ::sut/stopping-criteria
                    ::sut/future-events]))
   "A simple event execution advances id and iteration, bucket is advanced to the event bucket, state is modified. Execution stops with no future events as no more event are existing.")
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
             :stopping-criteria [#::sut{:id ::sut/no-future-events
                                        :doc ["No more future events to execute"]}]}
      (let [a-evt (fn [event-return _ _]
                    (-> event-return
                        (update-in [::sut/state :foo] (fnil inc 0))))]
        (-> #::sut{:sorter order-stub
                   :event-registry {:a a-evt}}
            (sut/initial-snapshot 0
                                  {}
                                  [#::sut{:type :a
                                          :bucket 2}
                                   #::sut{:type :a
                                          :bucket 5}])
            sut/continue
            (select-keys [::sut/id
                          ::sut/iteration
                          ::sut/bucket
                          ::sut/state
                          ::sut/past-events
                          ::sut/stopping-criteria
                          ::sut/future-events]))))
   "A doubled execution of the same event advances twice id and iteration. State is modified twice also. Past events are stored in the execution order.")
  (is
   (=
    #::sut{:bucket 0
           :future-events [#::sut{:type :a
                                  :bucket 5}]
           :id 2
           :iteration 2
           :state {}
           :past-events [:hey #::sut{:type :a
                                     :bucket 2}]
           :stopping-criteria [#::sut{:id ::sut/error-happens
                                      :doc ["Errors has been documented during event execution: %s"
                                            [{::sut/why :test}]]
                                      :current-event #::sut{:type :a
                                                            :bucket 2}
                                      :errors [{::sut/why :test}]}]}
    (-> #::sut{:sorter order-stub
               :event-registry {:a (fn [event-return _ _]
                                     (-> event-return
                                         (assoc ::sut/errors [#::sut{:why :test}])))}}
        (sut/initial-snapshot 0
                              {}
                              [#::sut{:type :a
                                      :bucket 2}
                               #::sut{:type :a
                                      :bucket 5}])
        ((fn [x] (assoc x ::sut/past-events [:hey])))
        sut/continue
        (select-keys [::sut/id
                      ::sut/iteration
                      ::sut/bucket
                      ::sut/state
                      ::sut/past-events
                      ::sut/stopping-criteria
                      ::sut/future-events])))
   "When an event returns an `error`, a stopping criteria is added, the event execution stops")
  (is
   (=
    #::sut{:id 3
           :iteration 3
           :bucket 10
           :state {:foo 2
                   :bar 1}
           :past-events [#::sut{:type :b
                                :bucket 2}
                         #::sut{:type :a
                                :bucket 10}]
           :future-events []
           :stopping-criteria [#::sut{:id ::sut/no-future-events
                                      :doc ["No more future events to execute"]}]}
    (let [a-evt (fn [event-return _ _]
                  (-> event-return
                      (update-in [::sut/state :foo] (comp inc (fnil inc 0)))))
          b-evt (fn [{::sut/keys [state future-events]} _ _]
                  #::sut{:future-events (conj future-events
                                              #::sut{:type :a
                                                     :bucket 10})
                         :state (update state :bar (fnil inc 0))})]
      (-> #::sut{:sorter order-stub
                 :event-registry {:a a-evt
                                  :b b-evt}}
          (sut/initial-snapshot 0
                                {}
                                [#::sut{:type :b
                                        :bucket 2}])
          sut/continue
          (select-keys [::sut/id
                        ::sut/iteration
                        ::sut/bucket
                        ::sut/state
                        ::sut/past-events
                        ::sut/stopping-criteria
                        ::sut/future-events]))))
   "An event creating an event is working. Each state update is executed. Pas-events are stored in the execution order.")
  (is
   (=
    #::sut{:id 1
           :iteration 1
           :bucket 10
           :state {}
           :past-events [#::sut{:type :a
                                :bucket 2}]
           :future-events []
           :stopping-criteria
           [#::sut{:id ::sut/causality-broken
                   :doc
                   ["Internal error - causality-broken: bucket is %d, next-event bucket is %d (event is `%s`)"
                    10
                    2
                    #::sut{:type :a
                           :bucket 2}]
                   :current-bucket 10
                   :event-bucket 2}]}
    (let [a-evt (fn [event-return _ _]
                  (-> event-return
                      (update-in [::sut/state :foo] (fnil inc 0))))]
      (-> #::sut{:sorter order-stub
                 :event-registry {:a a-evt}}
          (sut/initial-snapshot 10
                                {}
                                [#::sut{:type :a
                                        :bucket 2}])
          sut/continue
          (select-keys [::sut/id
                        ::sut/iteration
                        ::sut/bucket
                        ::sut/state
                        ::sut/past-events
                        ::sut/stopping-criteria
                        ::sut/future-events]))))
   "When causality is broken, there is no advancement on `id` and `iteration`. The faulty event is moved to `past-events`")
  (is (= #::sut{:bucket 1
                :future-events []
                :id 2
                :iteration 2
                :state {}
                :past-events [#::sut{:type :a
                                     :bucket 2}]
                :stopping-criteria [#::sut{:id ::sut/failed-event-execution
                                           :doc ["Internal error - failed execution of event"]
                                           :current-event #::sut{:type :a
                                                                 :bucket 2}
                                           :exception true}]}
         (update-in (let [a-evt (fn [_ _ _] (throw (ex-info "test" {})))]
                      (-> #::sut{:sorter order-stub
                                 :event-registry {:a a-evt}}
                          (sut/initial-snapshot 1
                                                {}
                                                [#::sut{:type :a
                                                        :bucket 2}])
                          sut/continue
                          (select-keys [::sut/id
                                        ::sut/iteration
                                        ::sut/bucket
                                        ::sut/state
                                        ::sut/past-events
                                        ::sut/stopping-criteria
                                        ::sut/future-events])))
                    [::sut/stopping-criteria 0 ::sut/exception]
                    some?))
      "When execution is failing, returns a failed-event-execution"))
