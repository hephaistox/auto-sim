(ns auto-sim.printer.rc-entity-route-test
  (:require
   [auto-sim.engine                  :as sim-engine]
   [auto-sim.entity                  :as sim-entity]
   [auto-sim.printer.rc-entity-route :as sut]
   [clojure.test                     :refer [deftest is]]))

;; ********************************************************************************
;; Internal API
;; ********************************************************************************

(deftest uuid-idx-test
  (is (= 0 (sut/uuid-idx (atom {}) :a)))
  (is (let [t (atom {})]
        (and (= 0 (sut/uuid-idx t :a))
             (= 0 (sut/uuid-idx t :a))
             (= 1 (sut/uuid-idx t :b))
             (= 0 (sut/uuid-idx t :a))
             (= 1 (sut/uuid-idx t :b))))))

(deftest check-test
  (is (sut/check :a :a))
  (is (not (sut/check :a :b)))
  (is (not (sut/check :a [:b :c])))
  (is (sut/check :a [:b :a :c])))

(deftest stopping-criteria*-test
  (is (= {:doc "because"
          :id :stopping-criteria}
         (sut/stopping-criteria* #::sim-engine{:id :because
                                               :doc ["because"]}))))

(deftest snapshot-iteration*-test
  (is (= {:iteration 2} (sut/snapshot-iteration* #::sim-engine{:iteration 2}))))

(deftest consumption*-test
  (is (= {:id :consumption
          :resource-id ::test
          :quantity 2
          :prio :high
          :entity-id 0
          :consumption-id 0}
         (sut/consumption* ::test
                           :uuid
                           #::sim-engine{:entity-id :uuid-1
                                         :consumption-quantity 2
                                         :priority :high}
                           (sut/create-translation)
                           (sut/create-translation)))))

(deftest event*-test
  (is (= ["%3d     create entity %02d (over %02d), next in %d" 2 3 nil nil]
         (sut/event* #::sim-engine{:type :a
                                   ::sim-entity/nb-entity 2
                                   :bucket 2}
                     {:entity-creation :a}
                     (sut/create-translation))))
  (is (= ["%3d/e%-2d create product" 2 0]
         (sut/event* #::sim-engine{:type :b
                                   ::sim-entity/nb-entity 2
                                   :bucket 2}
                     {:product-start :b}
                     (sut/create-translation))))
  (is (= ["%3d/e%-2d stops product" 2 0]
         (sut/event* #::sim-engine{:type :b
                                   ::sim-entity/nb-entity 2
                                   :bucket 2}
                     {:product-termination :b}
                     (sut/create-translation))))
  (is (= ["%3d/e%-2d next operation" 2 0]
         (sut/event* #::sim-engine{:type :b
                                   ::sim-entity/nb-entity 2
                                   :bucket 2}
                     {:route-next :b}
                     (sut/create-translation))))
  (is (nil? (sut/event* #::sim-engine{:type :b
                                      ::sim-entity/nb-entity 2
                                      :bucket 2}
                        {:skipped :b}
                        (sut/create-translation)))))


(deftest entity*-test
  (is (= "e0- (m1, 3), (m1, 7)"
         (sut/entity* [:foo-entity #::sim-engine{:created 3
                                                 :living 4
                                                 :entity-state {::sim-engine/route [{:m :m1
                                                                                     :pt 3}
                                                                                    {:m :m1
                                                                                     :pt 7}]}}]
                      (sut/create-translation)))))

(deftest queue*-test
  (is (= [["%s items seized by%3d/e%-2d create product" 2 1 0]
          ["%s items seized by%3d/e%-2d create product" 1 2 0]]
         (sut/queue* [#::sim-engine{:event #::sim-engine{:type :a
                                                         :bucket 1}
                                    :consumption-quantity 2}
                      #::sim-engine{:event #::sim-engine{:type :b
                                                         :bucket 2}
                                    :consumption-quantity 1}]
                     {:product-start [:a :b]}
                     (sut/create-translation)))))

(deftest resource*-test
  (is (= ["rsc `%s`: idle" "test"]
         (sut/resource* [::test #::sim-engine{:capacity 3
                                              :consumption {}
                                              :queue []
                                              :renewable? true}]
                        {:product-start [:a :b]}
                        (sut/create-translation)
                        (sut/create-translation)))
      "idle resource")
  (is (= [["rsc `%s`: busy, none is waiting" "test"]
          ["c(%s) (%s, %s=%s) - %s" 0 "test" 0 2 :high]
          ["c(%s) (%s, %s=%s) - %s" 1 "test" 0 2 :low]]
         (sut/resource* [::test #::sim-engine{:capacity 3
                                              :consumption
                                              {:uuid #::sim-engine{:entity-id :uuid-1
                                                                   :consumption-quantity 2
                                                                   :priority :high}
                                               :uuid-2 #::sim-engine{:entity-id :uuid-1
                                                                     :consumption-quantity 2
                                                                     :priority :low}}
                                              :queue []
                                              :renewable? true}]
                        {:product-start [:a :b]}
                        (sut/create-translation)
                        (sut/create-translation))))
  (is (= (sut/resource* [::test #::sim-engine{:capacity 3
                                              :consumption {:uuid
                                                            #::sim-engine{:entity-id :uuid-1
                                                                          :consumption-quantity 2
                                                                          :priority :high}}
                                              :queue []
                                              :renewable? true}]
                        {:product-start [:a :b]}
                        (sut/create-translation)
                        (sut/create-translation))
         (format "%s (%2d, %s) [%s] wait for [%s]" "test"
                 3 "r"
                 "[[\"c(%s) (%s, %s=%s) - %s\" 0 \"test\" 0 2 :high]]" "[]"))))

;;TODO I'm here, should test all parts of the print,
;;and use public API to assemble them, based on model itself

;; ********************************************************************************
;; Public API
;; ********************************************************************************

(deftest consumption-test
  (is (= (sut/consumption
          #::sim-engine{:state {::sim-engine/resource
                                {::test #::sim-engine{:capacity 3
                                                      :consumption
                                                      {:uuid #::sim-engine{:entity-id :uuid-1
                                                                           :consumption-quantity 2
                                                                           :priority :high}}
                                                      :queue []
                                                      :renewable? true}}}
                        :future-events [#::sim-engine{:type :a
                                                      :bucket 3
                                                      :entity-id :uuid-1}]}))))



(deftest past-events
  (is (= [] (sut/past-events* {} {:entity-creation :a} (sut/create-translation)))
      "Nothing to return")
  (is (= ["Simulation has stopped with" #:auto-sim.engine{:why :a}]
         (sut/past-events* {::sim-engine/stopping-criteria* [#::sim-engine{:why :a}]}
                           {:entity-creation :a}
                           (sut/create-translation)))
      "When an error occur")
  (is (= ["Simulation has stopped with" #:auto-sim.engine{:why :a}]
         (sut/past-events* #::sim-engine{:stopping-criteria* [#::sim-engine{:why :a}]
                                         :past-events [#::sim-engine{:type :a
                                                                     :bucket :b}]}
                           {:product-start :a}
                           (sut/create-translation)))
      "When an error occur"))
