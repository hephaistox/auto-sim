(ns auto-sim.rc.resource-test
  (:require
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [auto-sim.engine               :as-alias sim-engine]
   [auto-sim.rc.preemption-policy :refer [no-preemption]]
   [auto-sim.rc.resource          :as sut]
   [auto-sim.rc.unqueueing-policy :refer [fifo]]))

(deftest defaulting-values-test
  (is (= #::sim-engine{:capacity 1
                       :consumption {}
                       :queue []
                       :renewable? true}
         (sut/defaulting-values nil))
      "A purely defaulted value resource")
  (is (= #::sim-engine{:capacity 1
                       :consumption {}
                       :queue []
                       :renewable? true
                       :other-keys :are-allowed}
         (sut/defaulting-values #::sim-engine{:other-keys :are-allowed}))
      "Other data are kept"))

(deftest nb-consumed-capacity-test
  (is (zero? (sut/nb-consumed-resources nil))
      "With no currently seized, all the capacity is available")
  (is (zero? (sut/nb-consumed-resources #::sim-engine{:queue []}))
      "With no currently seized, all the capacity is available")
  (is (= 1 (sut/nb-consumed-resources #::sim-engine{:consumption {:a {:a :b}}}))
      "Defaulted consumption-quantity to 1")
  (is (= 8
         (sut/nb-consumed-resources #::sim-engine{:consumption
                                                  {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                                   {::sim-engine/consumption-quantity 3}
                                                   #uuid "33497220-f844-11ee-9fa1-17acea14e9de"
                                                   {::sim-engine/consumption-quantity 5}}}))
      "Currently seized are summed up"))

(deftest nb-available-resources-test
  (is (= 1 (sut/nb-available-resources nil)) "A non existing resource capacity is defaulted to 1")
  (is (= 7 (sut/nb-available-resources #::sim-engine{:capacity 7}))
      "With no currently seized, all the capacity is available")
  (is
   (= 9
      (sut/nb-available-resources #::sim-engine{:capacity 17
                                                :consumption
                                                {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                                 {::sim-engine/event #::sim-engine{:type ::c
                                                                                   :date 11}
                                                  ::sim-engine/consumption-quantity 3}
                                                 #uuid "33497220-f844-11ee-9fa1-17acea14e9de"
                                                 {:sim-engine/event #::sim-engine{:type ::d
                                                                                  :date 19}
                                                  ::sim-engine/consumption-quantity 5}}}))
   "Currently seized events are deduced from capacity, their seized quantity is taken into account")
  (is (zero? (sut/nb-available-resources #::sim-engine{:capacity 8
                                                       :consumption
                                                       {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                                        {::sim-engine/event #::sim-engine{:type ::c
                                                                                          :date 11}
                                                         ::sim-engine/consumption-quantity 3}
                                                        #uuid "33497220-f844-11ee-9fa1-17acea14e9de"
                                                        {::sim-engine/event #::sim-engine{:type ::d
                                                                                          :date 19}
                                                         ::sim-engine/consumption-quantity 5}}}))
      "If all resources are used, zero are available")
  (is (zero? (sut/nb-available-resources #::sim-engine{:capacity 7
                                                       :consumption
                                                       {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                                        {::sim-engine/event #::sim-engine{:type ::c
                                                                                          :date 11}
                                                         ::sim-engine/consumption-quantity 3}
                                                        #uuid "33497220-f844-11ee-9fa1-17acea14e9de"
                                                        {::sim-engine/event #::sim-engine{:type ::d
                                                                                          :date 19}
                                                         ::sim-engine/consumption-quantity 5}}}))
      "If more than all resources are used, zero are available"))

(deftest seize-test
  (testing "Seizing with enough capacity is consuming"
    (is (= #::sim-engine{:entity-id :entity-uuid
                         :priority :medium
                         :consumption-quantity 9}
           (let [res (sut/seize #::sim-engine{:capacity 13}
                                #::sim-engine{:a :b
                                              :entity-id :entity-uuid}
                                9
                                :medium)
                 {:keys [consumption-uuid resource]} res]
             (get-in resource [::sim-engine/consumption consumption-uuid])))
        "Seizing one resource with enough capacity available")
    (is (= #::sim-engine{:entity-id :entity-uuid
                         :priority :medium
                         :consumption-quantity 9}
           (let [res (sut/seize #::sim-engine{:capacity 9}
                                #::sim-engine{:a :b
                                              :entity-id :entity-uuid}
                                9
                                :medium)
                 {:keys [consumption-uuid resource]} res]
             (get-in resource [::sim-engine/consumption consumption-uuid])))
        "Seizing one resource with the exact capacity available")
    (is (= #::sim-engine{:entity-id :entity-uuid
                         :priority :medium
                         :consumption-quantity 1}
           (let [res (sut/seize #::sim-engine{}
                                #::sim-engine{:a :b
                                              :entity-id :entity-uuid}
                                1
                                :medium)
                 {:keys [consumption-uuid resource]} res]
             (get-in resource [::sim-engine/consumption consumption-uuid])))
        "No capacity defined for the resource is defaulted to 1")
    (is (= {:resource {}
            :errors [#::sim-engine{:why :consumption-quantity-wrong
                                   :event #::sim-engine{:a :b
                                                        :entity-id :entity-uuid}
                                   :resource {}
                                   :consumption-quantity -1}]}
           (sut/seize #::sim-engine{}
                      #::sim-engine{:a :b
                                    :entity-id :entity-uuid}
                      -1
                      :medium))
        "Errors are returned"))
  (testing "Seizing with missing capacity returns `nil` consumption and adds it to the queue."
    (is (= {:resource #::sim-engine{:capacity 0
                                    :queue [#::sim-engine{:event #::sim-engine{:entity-id
                                                                               :entity-uuid
                                                                               :a :b}
                                                          :priority :high
                                                          :consumption-quantity 1}]}}
           (sut/seize #::sim-engine{:capacity 0}
                      #::sim-engine{:entity-id :entity-uuid
                                    :a :b}
                      1
                      :high))
        "A resource with zero capacity is queueing all events")
    (is (= {:resource #::sim-engine{:capacity 12}
            :errors [#::sim-engine{:why :consumption-quantity-wrong
                                   :event #::sim-engine{:entity-id :entity-uuid
                                                        :a :b}
                                   :resource #::sim-engine{:capacity 12}
                                   :consumption-quantity -20}]}
           (sut/seize #::sim-engine{:capacity 12}
                      #::sim-engine{:entity-id :entity-uuid
                                    :a :b}
                      -20
                      :high))
        "An error is seizing is returned.")
    (is (= {:resource #::sim-engine{:capacity 12
                                    :queue [#::sim-engine{:event #::sim-engine{:entity-id
                                                                               :entity-uuid
                                                                               :a :b}
                                                          :priority :high
                                                          :consumption-quantity 20}]}}
           (sut/seize #::sim-engine{:capacity 12}
                      #::sim-engine{:entity-id :entity-uuid
                                    :a :b}
                      20
                      :high))
        "A too high capacity required is queueing all events")))

(deftest dispose-test
  (is (= {:resource {}
          :errors [#::sim-engine{:why :consumption-uuid-does-not-exist
                                 :resource {}
                                 :consumption-uuid :a}]}
         (sut/dispose {} fifo :non-existing-entity-uuid))
      "Disposing a non-existing resource is noop on the resource and documents an error")
  (is (= {:resource #::sim-engine{:capacity 20
                                  :consumption {}
                                  :queue []}}
         (sut/dispose #::sim-engine{:capacity 20
                                    :consumption {:aa #::sim-engine{:consumption-quantity 13}}
                                    :queue []}
                      :aa
                      fifo))
      "Dispose the last consumption, no other event is pending")
  (is (= {:unqueued [#::sim-engine{:event {:a :b}
                                   :consumption-quantity 12}]
          :resource #::sim-engine{:capacity 20
                                  :consumption {}
                                  :queue nil}}
         (sut/dispose #::sim-engine{:capacity 20
                                    :consumption {:aa #::sim-engine{:consumption-quantity 13}}
                                    :queue [#::sim-engine{:event {:a :b}
                                                          :consumption-quantity 12}]}
                      :aa
                      fifo))
      "Dispose an existing event, an other event is still pending"))

{:consumption-uuid #uuid "019514ef-9077-804f-8205-9ed5cd004ffa"
 :resource #::sim-engine{:capacity 9
                         :consumption {#uuid "019514ef-9077-804f-8205-9ed5cd004ffa"
                                       #::sim-engine{:consumption-quantity 9
                                                     :entity-id :entity-uuid
                                                     :priority :medium}}}}

(deftest dispose-seize-test
  (let [resource (-> #::sim-engine{:capacity 1}
                     (sut/seize #::sim-engine{:entity-id :entity-uuid
                                              :a :b1}
                                1
                                :high)
                     :resource
                     (sut/seize #::sim-engine{:entity-id :entity-uuid
                                              :a :b2}
                                2
                                :high)
                     :resource
                     (sut/seize #::sim-engine{:entity-id :entity-uuid
                                              :a :b3}
                                3
                                :high)
                     :resource)
        consumption-uuid (-> resource
                             ::sim-engine/consumption
                             ffirst)]
    (is (= {:resource #::sim-engine{:consumption {}
                                    :capacity 1
                                    :queue
                                    [#::sim-engine{:event #::sim-engine{:entity-id :entity-uuid
                                                                        :a :b2}
                                                   :priority :high
                                                   :consumption-quantity 2}
                                     #::sim-engine{:event #::sim-engine{:entity-id :entity-uuid
                                                                        :a :b3}
                                                   :priority :high
                                                   :consumption-quantity 3}]}
            :unqueued []}
           (sut/dispose resource consumption-uuid fifo))
        "Disposing 1 which is not enough for next event"))
  (let [resource (-> #::sim-engine{:capacity 2}
                     (sut/seize #::sim-engine{:entity-id :entity-uuid
                                              :a :b1}
                                1
                                :high)
                     :resource
                     (sut/seize #::sim-engine{:entity-id :entity-uuid
                                              :a :b2}
                                2
                                :high)
                     :resource
                     (sut/seize #::sim-engine{:entity-id :entity-uuid
                                              :a :b3}
                                3
                                :high)
                     :resource)
        consumption-uuid (-> resource
                             ::sim-engine/consumption
                             ffirst)]
    (is (= {:unqueued [#::sim-engine{:event #::sim-engine{:entity-id :entity-uuid
                                                          :a :b2}
                                     :priority :high
                                     :consumption-quantity 2}]
            :resource #::sim-engine{:consumption {}
                                    :capacity 2
                                    :queue [#::sim-engine{:event #::sim-engine{:a :b3
                                                                               :entity-id
                                                                               :entity-uuid}
                                                          :priority :high
                                                          :consumption-quantity 3}]}}
           (sut/dispose resource consumption-uuid fifo))
        "There is 1 capacity left, and 1 disposed, which is enough for the next one")))

(deftest update-capacity-test
  (is (= {:resource #::sim-engine{:capacity 7
                                  :queue []}}
         (sut/update-capacity {} no-preemption fifo 7)
         (sut/update-capacity nil no-preemption fifo 7)
         (sut/update-capacity #::sim-engine{:capacity 5} no-preemption fifo 7))
      "Empty resource is updated to 7")
  (is (= 14
         (-> #::sim-engine{:capacity 1}
             (sut/update-capacity no-preemption fifo 14)
             :resource
             ::sim-engine/capacity))
      "A resource is updated with the `new-capacity`")
  (is (= {:resource #::sim-engine{:capacity 12
                                  :queue []}}
         (sut/update-capacity #::sim-engine{:capacity 5} no-preemption fifo 12))
      "Preemption is defaulted to no-preemption"))
