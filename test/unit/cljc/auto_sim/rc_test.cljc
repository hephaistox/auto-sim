(ns auto-sim.rc-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.engine                  :as-alias sim-engine]
   [auto-sim.rc                      :as sut]
   [auto-sim.rc.preemption-policy    :refer [no-preemption]]
   [auto-sim.rc.resource.consumption :as sim-rc-consumption]
   [auto-sim.rc.unqueueing-policy    :refer [fifo]]))

(deftest define-resource-test
  (is (= {::sim-engine/state {::sim-engine/resource {:foo #::sim-engine{:resource :content
                                                                        :capacity 1
                                                                        :consumption {}
                                                                        :queue []
                                                                        :renewable? true}}}}
         (sut/define-resource {} nil 0 :foo #::sim-engine{:resource :content}))))

(deftest seize-test
  (is (= {::sim-engine/errors [#::sim-engine{:why :resource-not-found
                                             :resource-name :auto-sim.rc-test/test
                                             :quantity 1
                                             :possible-resources []}]}
         (sut/seize {} nil 3 ::test 1 {} :high))
      "If the `resource-name` is not already defined in the `state`, the seizing is noop")
  (is
   (= #::sim-engine{:state {::sim-engine/resource
                            {::test #::sim-engine{:capacity 1
                                                  :consumption {}
                                                  :queue [#::sim-engine{:event {:event :seizing}
                                                                        :priority :high
                                                                        :consumption-quantity 2}]
                                                  :renewable? true}}}}
      (-> {}
          (sut/define-resource nil 0 ::test {})
          (sut/seize nil 3 ::test 2 {:event :seizing} :high)))
   "Seizing an non-available resource postpones the event execution. So it is in the queue and not in the future-events")
  (is
   (= #::sim-engine{:state {::sim-engine/resource
                            {::test #::sim-engine{:capacity 3
                                                  :consumption
                                                  {:uuid #::sim-engine{:entity-id :uuid-1
                                                                       :consumption-quantity 2
                                                                       :priority :high}}
                                                  :queue []
                                                  :renewable? true}}}
                    :future-events [#::sim-engine{:type :a
                                                  :bucket 3
                                                  :entity-id :uuid-1}]}
      (-> {}
          (sut/define-resource nil nil ::test {::sim-engine/capacity 3})
          (sut/seize {::sim-engine/entity-id :uuid-1} 3 ::test 2 #::sim-engine{:type :a} :high)
          (update-in [::sim-engine/state ::sim-engine/resource ::test ::sim-engine/consumption]
                     update-keys
                     (fn [_] :uuid))))
   "When seizing an available resource, the state stores that consumption, and the event is scheduled now."))

(deftest dispose-test
  (is
   (=
    {::sim-engine/errors [#::sim-engine{:why :resource-not-found
                                        :resource-name :auto-sim.rc-test/non-existing-resource
                                        :quantity 3}]}
    (->
      {}
      (sut/dispose nil 7 ::non-existing-resource 3 (sim-rc-consumption/compare-by-order []) fifo)))
   "Disposing a non existing resource is noop")
  (is (= #::sim-engine{:state #::sim-engine{:resource {::test #::sim-engine{:capacity 3
                                                                            :consumption {}
                                                                            :queue []
                                                                            :renewable? true}}}
                       :errors [#::sim-engine{:why :cant-dispose-quantity
                                              :capacity 3
                                              :consumption-uuid nil
                                              :quantity 3
                                              :quantity-to-dispose 3}]}
         (-> {}
             (sut/define-resource nil nil ::test {::sim-engine/capacity 3})
             (sut/dispose nil 7 ::test 3 (sim-rc-consumption/compare-by-order []) fifo)))
      "Disposing more than what is seized")
  (is (= #::sim-engine{:state {::sim-engine/resource {::test #::sim-engine{:capacity 3
                                                                           :consumption {}
                                                                           :queue []
                                                                           :renewable? true}}}
                       :future-events [{:event :a
                                        :auto-sim.engine/entity-id :entity-1
                                        :auto-sim.engine/bucket 0}]}
         (-> {}
             (sut/define-resource nil nil ::test {::sim-engine/capacity 3})
             (sut/seize {::sim-engine/entity-id :entity-1} 0 ::test 3 {:event :a} :high)
             (sut/dispose nil 7 ::test 3 (sim-rc-consumption/compare-by-order []) fifo)))
      "Disposing exactly what is seized")
  (is (= 2
         (-> {}
             (sut/define-resource nil nil ::test {::sim-engine/capacity 5})
             (sut/seize {::sim-engine/entity-id :entity-1} 0 ::test 5 {:event :a} :high)
             (sut/dispose nil 7 ::test 3 (sim-rc-consumption/compare-by-order []) fifo)
             (sut/nb-consumed-resources nil nil ::test)))
      "Disposing a part of what is seized")
  ;;
  (is "Disposing a resource with blocked events release them"))

(deftest update-capacity-test
  (is (= {::sim-engine/errors [#::sim-engine{:why :resource-not-found
                                             :resource-name :auto-sim.rc-test/test
                                             :new-capacity 7}]}
         (->
           {}
           (sut/update-capacity {::sim-engine/entity-id :entity-1} 2 ::test 7 no-preemption fifo)))
      "Non existing resource are documented as an error")
  (is (= #::sim-engine{:state #::sim-engine{:resource {::test #::sim-engine{:capacity 7
                                                                            :consumption {}
                                                                            :renewable? true
                                                                            :queue []}}}}
         (-> {}
             (sut/define-resource nil nil ::test {::sim-engine/capacity 3})
             (sut/update-capacity #::sim-engine{:resource {::test #::sim-engine{:capacity 5
                                                                                :queue []}}}
                                  []
                                  ::test
                                  7
                                  no-preemption
                                  fifo)))
      "Existing resource is updated"))

