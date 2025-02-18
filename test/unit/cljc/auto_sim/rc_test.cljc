(ns auto-sim.rc-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.engine               :as-alias sim-engine]
   [auto-sim.rc                   :as sut]
   [auto-sim.rc.preemption-policy :refer [no-preemption]]
   [auto-sim.rc.unqueueing-policy :refer [fifo]]))

(deftest define-resource-test
  (is (= {::sim-engine/resource {:foo #::sim-engine{:resource :content
                                                    :capacity 1
                                                    :consumption {}
                                                    :queue []
                                                    :renewable? true}}}
         (sut/define-resource {} :foo #::sim-engine{:resource :content}))))

(deftest seize-test
  (is (= [{:state :a} []] (sut/seize {:state :a} [] 1 {} ::test 1))
      "If the `resource-name` is not already defined in the `state`, the seizing is noop")
  (is
   (= [{::sim-engine/resource
        {::test #::sim-engine{:capacity 1
                              :consumption {}
                              :queue [#::sim-engine{:event #::sim-engine{:type :a
                                                                         :entity-id :uuid-1
                                                                         :bucket 1}
                                                    :consumption-quantity 2}]
                              :renewable? true}}}
       []]
      (-> {}
          (sut/define-resource ::test {})
          (sut/seize []
                     1 #::sim-engine{:type :a
                                     :entity-id :uuid-1}
                     ::test 2)))
   "Seizing an unavailable resource postpones the event execution. So it is in the queue and not in the future-events")
  (is (= {::sim-engine/resource {::test #::sim-engine{:capacity 3
                                                      :consumption
                                                      {:uuid #::sim-engine{:entity-id :uuid-1
                                                                           :consumption-quantity 2}}
                                                      :queue []
                                                      :renewable? true}}}
         (-> {}
             (sut/define-resource ::test {::sim-engine/capacity 3})
             (sut/seize []
                        1 #::sim-engine{:type :a
                                        :entity-id :uuid-1}
                        ::test 2)
             first
             (update-in [::sim-engine/resource ::test ::sim-engine/consumption]
                        update-keys
                        (fn [_] :uuid))))
      "When seizing an available resource, the state stores that consumption.")
  (is (= [#::sim-engine{:type :a
                        :bucket 1
                        :entity-id :uuid-1
                        ::sim-engine/resource {::test :uuid}}]
         (mapv (fn [event] (assoc-in event [::sim-engine/resource ::test] :uuid))
               (-> {}
                   (sut/define-resource ::test {::sim-engine/capacity 3})
                   (sut/seize []
                              1 #::sim-engine{:type :a
                                              :entity-id :uuid-1}
                              ::test 2)
                   second)))
      "When seizing an available resource, the event is inserted in `future-events`"))

(deftest dispose-test
  (is (= [{} []]
         (sut/dispose {}
                      []
                      #:sim-engine{:type :a
                                   :date 1}
                      ::a
                      fifo))
      "Disposing a non existing resource is noop")
  (is (= [{::sim-engine/resource {::test #::sim-engine{:consumption #::sim-engine{:type :a
                                                                                  :entity-id
                                                                                  :uuid-1}
                                                       :queue []}}}
          []]
         (-> {}
             (sut/define-resource ::test {::sim-engine/capacity 2})
             (sut/seize []
                        ::test 2
                        1 #::sim-engine{:type :a
                                        :entity-id :uuid-1})
             first
             (sut/dispose [] #::sim-engine{} ::test fifo)))
      "Disposing an existing resource, currently consumption is removing it")
  (is "Disposing a resource with blocked events release them"))

(deftest resource-update-test
  (is (= #:sim-engine{:state #::sim-engine{:resource {::test #::sim-engine{:capacity 7
                                                                           :queue []}}}
                      :future-events []}
         (sut/resource-update {} [] ::test 7 no-preemption fifo))
      "Non existing resource is created")
  (is (= #:sim-engine{:state #::sim-engine{:resource {::test #::sim-engine{:capacity 7
                                                                           :queue []}}}
                      :future-events []}
         (sut/resource-update #::sim-engine{:resource {::test #::sim-engine{:capacity 5
                                                                            :queue []}}}
                              []
                              ::test
                              7
                              no-preemption
                              fifo))
      "Existing resource is updated"))

#_(defn- wo-initial-snapshot [model] (dissoc model ::sim-engine/initial-snapshot))

(defn- resources-kw
  [model]
  (-> model
      (get-in [::sim-engine/initial-snapshot ::sim-engine/state ::sim-engine/resource])
      keys
      set))

#_(deftest wrap-model-test
    (testing "If no resource is defined, doesn't change the model"
      (is (nil? (sut/wrap-model nil nil nil)))
      (is (nil? (sut/wrap-model nil nil nil))))
    (is (= [{:a :b
             ::sim-engine/model-data {::sim-engine/rc {:ra nil
                                                       :rb {}}}}
            #{:ra :rb}]
           ((juxt wo-initial-snapshot resources-kw)
            (-> {:a :b
                 ::sim-engine/model-data {::sim-engine/rc {:ra nil
                                                           :rb {}}}}
                (sut/wrap-model nil nil))))
        "Resources are added")
    (is (= [{:a :b
             ::sim-engine/model-data {::sim-engine/rc {:rc nil
                                                       :rd {}}}}
            #{:ra :rb :rc :rd}]
           ((juxt wo-initial-snapshot resources-kw)
            (-> {:a :b
                 ::sim-engine/initial-snapshot {::sim-engine/state {::sim-engine/resource {:ra :ra
                                                                                           :rb
                                                                                           :rb}}}
                 ::sim-engine/model-data {::sim-engine/rc {:rc nil
                                                           :rd {}}}}
                (sut/wrap-model nil nil))))
        "Existing data are not overidden"))
