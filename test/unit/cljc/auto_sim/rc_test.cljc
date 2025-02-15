(ns auto-sim.rc-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.engine               :as-alias sim-engine]
   [auto-sim.rc                   :as sut]
   [auto-sim.rc.unblocking-policy :as sut-unblocking-policy]))

(deftest define-resource-test
  (is (= #::sut{:resource {:foo #::sut{:resource :content
                                       :capacity 1
                                       :currently-consuming {}
                                       :queue []
                                       :renewable? true}}}
         (sut/define-resource {} :foo #::sut{:resource :content}))))

(deftest seizing-resource-test
  (is (= [{:state :a} []] (sut/seize {:state :a} [] ::test 1 1 {}))
      "If the `resource-name` is not already defined in the `state`, the seizing is noop")
  (is
   (= [#::sut{:resource {::test #::sut{:capacity 1
                                       :currently-consuming {}
                                       :queue [#::sut{:seizing-event #::sim-engine{:type :a
                                                                                   :bucket 1}
                                                      :consumed-quantity 2}]
                                       :renewable? true}}}
       []]
      (-> {}
          (sut/define-resource ::test {})
          (sut/seize [] ::test 2 1 #::sim-engine{:type :a})))
   "Seizing an unavailable resource postpones the event execution. So it is in the queue and no in the future-events")
  (is
   (= [#::sut{:resource {::test #::sut{:capacity 3
                                       :currently-consuming {:uuid #::sut{:seizing-event
                                                                          #::sim-engine{:type :a
                                                                                        :bucket 1}
                                                                          :consumed-quantity 2}}
                                       :queue []
                                       :renewable? true}}}
       [#::sim-engine{:type :a
                      :bucket 1
                      ::sut/resource {::test :uuid}}]]
      (let [[state future-events] (-> {}
                                      (sut/define-resource ::test {::sut/capacity 3})
                                      (sut/seize [] ::test 2 1 #::sim-engine{:type :a}))]
        [(update-in state
                    [::sut/resource ::test ::sut/currently-consuming]
                    update-keys
                    (fn [_] :uuid))
         (mapv (fn [event] (assoc-in event [::sut/resource ::test] :uuid)) future-events)]))
   "When seizing an available resource, the event is inserted in `future-events`, the state stores that consumption."))

#_(deftest resource-dispose-test
    (is (= #:automaton-simulation-de.simulation-engine{:state nil}
           (sut/dispose {}
                        ::a
                        #:automaton-simulation-de.simulation-engine{:type :a
                                                                    :date 1}))
        "Disposing a non existing resource is noop")
    (is (= #:automaton-simulation-de.simulation-engine{:state #::sut{:resource
                                                                     {::test
                                                                      #::sut{:currently-consuming {}
                                                                             :queue []}}}
                                                       :future-events []}
           (-> #:automaton-simulation-de.simulation-engine{:state
                                                           #::sut{:resource
                                                                  {::test
                                                                   #::sut{:currently-consuming
                                                                          {1
                                                                           #::sut{:seizing-event
                                                                                  {:a :b}
                                                                                  :consumed-quantity
                                                                                  1}}
                                                                          :queue []}}}
                                                           :future-events []}
               (dispose-cacheproof ::test {::sut/resource {::test 1}})))
        "Disposing an existing resource, currently consuming is removing it")
    (is
     (=
      #:automaton-simulation-de.simulation-engine{:state
                                                  #::sut{:resource
                                                         {::test
                                                          #::sut{:currently-consuming
                                                                 {1
                                                                  #::sut{:seizing-event
                                                                         #:automaton-simulation-de.simulation-engine{:type
                                                                                                                     :a
                                                                                                                     :date
                                                                                                                     2}
                                                                         :consumed-quantity 2}}
                                                                 :capacity 3
                                                                 :preemption-policy
                                                                 ::sut/no-preemption
                                                                 :renewable? true
                                                                 :unblocking-policy ::sut/FIFO
                                                                 :queue
                                                                 [#::sut{:seizing-event
                                                                         #:automaton-simulation-de.simulation-engine{:type
                                                                                                                     :b
                                                                                                                     :date
                                                                                                                     2}
                                                                         :consumed-quantity 3}]}}}
                                                  :future-events
                                                  [#:automaton-simulation-de.simulation-engine{:type
                                                                                               :a
                                                                                               :date
                                                                                               2
                                                                                               ::sut/resource
                                                                                               {::test
                                                                                                1}}]}
      (let [event-return (-> (default-resource-event-return ::test 3)
                             (sut/seize ::test 2
                                        2 #:automaton-simulation-de.simulation-engine{:type :a
                                                                                      :date 2})
                             (sut/seize ::test 3
                                        2 #:automaton-simulation-de.simulation-engine{:type :b
                                                                                      :date 2}))
            a-currently-consuming-event (-> event-return
                                            ::sim-engine/future-events
                                            first)]
        (-> event-return
            (remove-consumption-uuid ::test)
            (dispose-cacheproof ::test a-currently-consuming-event))))
     "Disposing a resource with blocked events release them"))

#_(deftest resource-update-test
    (is (= #:automaton-simulation-de.simulation-engine{:state #::sut{:resource {::test
                                                                                #::sut{:capacity 7
                                                                                       :queue []}}}
                                                       :future-events []}
           (resource-update-cacheproof #:automaton-simulation-de.simulation-engine{:state {}
                                                                                   :future-events
                                                                                   []}
                                       ::test
                                       7))
        "Non existing resource is created")
    (is (= #:automaton-simulation-de.simulation-engine{:state #::sut{:resource {::test
                                                                                #::sut{:capacity 7
                                                                                       :queue []}}}
                                                       :future-events []}
           (-> #:automaton-simulation-de.simulation-engine{:state #::sut{:resource
                                                                         {::test #::sut{:capacity 5
                                                                                        :queue []}}}
                                                           :future-events []}
               (resource-update-cacheproof ::test 7)))
        "Existing resource is updated"))

#_(defn- wo-initial-snapshot [model] (dissoc model ::sim-engine/initial-snapshot))

(defn- resources-kw
  [model]
  (-> model
      (get-in [::sim-engine/initial-snapshot ::sim-engine/state ::sut/resource])
      keys
      set))

#_(deftest wrap-model-test
    (testing "If no resource is defined, doesn't change the model"
      (is (nil? (sut/wrap-model nil nil nil)))
      (is (nil? (sut/wrap-model nil nil nil))))
    (is (= [{:a :b
             ::sim-engine/model-data {::sut/rc {:ra nil
                                                :rb {}}}}
            #{:ra :rb}]
           ((juxt wo-initial-snapshot resources-kw)
            (-> {:a :b
                 ::sim-engine/model-data {::sut/rc {:ra nil
                                                    :rb {}}}}
                (sut/wrap-model nil nil))))
        "Resources are added")
    (is (= [{:a :b
             ::sim-engine/model-data {::sut/rc {:rc nil
                                                :rd {}}}}
            #{:ra :rb :rc :rd}]
           ((juxt wo-initial-snapshot resources-kw)
            (-> {:a :b
                 ::sim-engine/initial-snapshot {::sim-engine/state {::sut/resource {:ra :ra
                                                                                    :rb :rb}}}
                 ::sim-engine/model-data {::sut/rc {:rc nil
                                                    :rd {}}}}
                (sut/wrap-model nil nil))))
        "Existing data are not overidden"))
