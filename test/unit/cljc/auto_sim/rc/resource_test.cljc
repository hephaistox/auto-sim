(ns auto-sim.rc.resource-test
  (:require
   [auto-sim.rc                   :as-alias sim-rc]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])
   [auto-sim.rc.preemption-policy :refer [no-preemption]]
   [auto-sim.rc.resource          :as sut]
   [auto-sim.rc.unblocking-policy :refer [fifo]]
   [auto-sim.simulation-engine    :as-alias sim-engine]))

(deftest defaulting-values-test
  (is (= #::sim-rc{:capacity 1
                   :currently-consuming {}
                   :queue []
                   :renewable? true}
         (sut/defaulting-values nil))
      "A purely defaulted value resource")
  (is (= #::sim-rc{:capacity 1
                   :currently-consuming {}
                   :queue []
                   :renewable? true
                   :other-keys :are-allowed}
         (sut/defaulting-values #::sim-rc{:other-keys :are-allowed}))
      "Other data are kept"))

(deftest nb-consumed-capacity-test
  (testing "With no currently seized, all the capacity is available"
    (is (zero? (sut/nb-consumed-resources nil)))
    (is (zero? (sut/nb-consumed-resources #::sim-rc{:queue []}))))
  (is (= 1 (sut/nb-consumed-resources #::sim-rc{:currently-consuming {:a {:a :b}}}))
      "Defaulted consumed-quantity to 1")
  (is (= 8
         (sut/nb-consumed-resources
          #::sim-rc{:currently-consuming
                    {#uuid "33497220-f844-11ee-9fa1-17acea14e9df" {::sim-rc/consumed-quantity 3}
                     #uuid "33497220-f844-11ee-9fa1-17acea14e9de" {::sim-rc/consumed-quantity 5}}}))
      "Currently seized are summed up"))

(deftest nb-available-resources-test
  (is (= 1 (sut/nb-available-resources nil)) "A non existing resource capacity is defaulted to 1")
  (is (= 7 (sut/nb-available-resources #::sim-rc{:capacity 7}))
      "With no currently seized, all the capacity is available")
  (is
   (= 9
      (sut/nb-available-resources #::sim-rc{:capacity 17
                                            :currently-consuming
                                            {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                             {::sim-rc/event #::sim-engine{:type ::c
                                                                           :date 11}
                                              ::sim-rc/consumed-quantity 3}
                                             #uuid "33497220-f844-11ee-9fa1-17acea14e9de"
                                             {::sim-rc/event #::sim-engine{:type ::d
                                                                           :date 19}
                                              ::sim-rc/consumed-quantity 5}}}))
   "Currently seized events are deduced from capacity, their seized quantity is taken into account")
  (testing "If all resources are used, zero are available"
    (is (zero? (sut/nb-available-resources #::sim-rc{:capacity 8
                                                     :currently-consuming
                                                     {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                                      {::sim-rc/event #::sim-engine{:type ::c
                                                                                    :date 11}
                                                       ::sim-rc/consumed-quantity 3}
                                                      #uuid "33497220-f844-11ee-9fa1-17acea14e9de"
                                                      {::sim-rc/event #::sim-engine{:type ::d
                                                                                    :date 19}
                                                       ::sim-rc/consumed-quantity 5}}})))
    (is (zero? (sut/nb-available-resources #::sim-rc{:capacity 7
                                                     :currently-consuming
                                                     {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                                      {::sim-rc/event #::sim-engine{:type ::c
                                                                                    :date 11}
                                                       ::sim-rc/consumed-quantity 3}
                                                      #uuid "33497220-f844-11ee-9fa1-17acea14e9de"
                                                      {::sim-rc/event #::sim-engine{:type ::d
                                                                                    :date 19}
                                                       ::sim-rc/consumed-quantity 5}}})))))

(deftest seize-test
  (testing "Seizing one resource with that capacity already available"
    (is (= #::sim-rc{:seizing-event {:a :b}
                     :consumed-quantity 9}
           (let [[consumption-uuid resource] (sut/seize #::sim-rc{:capacity 13} 9 {:a :b})]
             (get-in resource [::sim-rc/currently-consuming consumption-uuid]))))
    (is (= #::sim-rc{:seizing-event {:a :b}
                     :consumed-quantity 9}
           (let [[consumption-uuid resource] (sut/seize #::sim-rc{:capacity 9} 9 {:a :b})]
             (get-in resource [::sim-rc/currently-consuming consumption-uuid]))))
    (is (= #::sim-rc{:seizing-event {:a :b}
                     :consumed-quantity 1}
           (let [[consumption-uuid resource] (sut/seize #::sim-rc{} 1 {:a :b})]
             (get-in resource [::sim-rc/currently-consuming consumption-uuid])))))
  (testing "Seizing one resource with capacity missing"
    (is (nil? (first (sut/seize #::sim-rc{:capacity 0} 1 {:a :b}))))
    (is (nil? (first (sut/seize #::sim-rc{:capacity 12} 20 {:a :b}))))
    (is (empty? (-> (sut/seize #::sim-rc{:capacity 12} 20 {:a :b})
                    second
                    ::sim-rc/currently-consuming)))
    (is (= #::sim-rc{:capacity 12
                     :queue [#::sim-rc{:seizing-event {:a :b}
                                       :consumed-quantity 20}]}
           (-> (sut/seize #::sim-rc{:capacity 12} 20 {:a :b})
               second)))
    (is (-> (sut/seize #::sim-rc{:capacity 12} 20 {:a :b})
            first
            nil?))))

(deftest dispose-test
  (is (= [[]
          #::sim-rc{:currently-consuming {}
                    :queue []}]
         (sut/dispose {} 1 fifo))
      "Dispose a non existing resource is noop")
  (is (= [[]
          #::sim-rc{:capacity 20
                    :currently-consuming {}
                    :queue []}]
         (sut/dispose #::sim-rc{:capacity 20
                                :currently-consuming {:aa #::sim-rc{:consumed-quantity 13}}
                                :queue []}
                      :aa
                      fifo))
      "Dispose an existing event, no other event is pending")
  (is (= [[#::sim-rc{:seizing-event {:a :b}
                     :consumed-quantity 12}]
          #::sim-rc{:capacity 20
                    :currently-consuming {}
                    :queue []}]
         (sut/dispose #::sim-rc{:capacity 20
                                :currently-consuming {:aa #::sim-rc{:consumed-quantity 13}}
                                :queue [#::sim-rc{:seizing-event {:a :b}
                                                  :consumed-quantity 12}]}
                      :aa
                      fifo))
      "Dispose an existing event, an other event is still pending"))

(deftest dispose-seize-test
  (let [resource (-> #::sim-rc{:capacity 1}
                     (sut/seize 1 {:a :b1})
                     second
                     (sut/seize 2 {:a :b2})
                     second
                     (sut/seize 3 {:a :b3})
                     second)
        consumption-uuid (-> resource
                             ::sim-rc/currently-consuming
                             ffirst)]
    (is (= [[]
            #::sim-rc{:currently-consuming {}
                      :capacity 1
                      :queue [#::sim-rc{:seizing-event {:a :b2}
                                        :consumed-quantity 2}
                              #::sim-rc{:seizing-event {:a :b3}
                                        :consumed-quantity 3}]}]
           (sut/dispose resource consumption-uuid fifo))
        "Disposing 1 which is not enough for next event"))
  (let [resource (-> #::sim-rc{:capacity 2}
                     (sut/seize 1 {:a :b1})
                     second
                     (sut/seize 2 {:a :b2})
                     second
                     (sut/seize 3 {:a :b3})
                     second)
        consumption-uuid (-> resource
                             ::sim-rc/currently-consuming
                             ffirst)]
    (is (= [[#::sim-rc{:seizing-event {:a :b2}
                       :consumed-quantity 2}]
            #::sim-rc{:currently-consuming {}
                      :capacity 2
                      :queue [#::sim-rc{:seizing-event {:a :b3}
                                        :consumed-quantity 3}]}]
           (sut/dispose resource consumption-uuid fifo))
        "There is 1 capacity left, and 1 disposed, which is enough for the next one")))

(deftest update-capacity-test
  (is (= [[]
          #::sim-rc{:capacity 7
                    :queue []}]
         (sut/update-capacity {} 7 no-preemption fifo)
         (sut/update-capacity nil 7 no-preemption fifo)
         (sut/update-capacity #::sim-rc{:capacity 5} 7 no-preemption fifo))
      "Empty resource is updated to 7")
  (is (= 14
         (-> #::sim-rc{:capacity 1}
             (sut/update-capacity 14 no-preemption fifo)
             second
             ::sim-rc/capacity))
      "A resource is updated with the `new-capacity`")
  (testing "Preemption is defaulted to no-preemption"
    (is (= [[]
            #::sim-rc{:capacity 12
                      :queue []}]
           (sut/update-capacity #::sim-rc{:capacity 5} 12 no-preemption fifo)))))
