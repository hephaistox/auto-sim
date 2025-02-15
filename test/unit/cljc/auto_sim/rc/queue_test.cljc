(ns auto-sim.rc.queue-test
  (:require
   #?(:clj [clojure.test :refer [are deftest is testing]]
      :cljs [cljs.test :refer [are deftest is testing] :include-macros true])
   [auto-sim.engine               :as-alias sim-engine]
   [auto-sim.rc                   :as-alias sim-rc]
   [auto-sim.rc.queue             :as sut]
   [auto-sim.rc.unblocking-policy :as sim-rc-unblocking-policy]))

(deftest queue-event-test
  (is (= {} (sut/queue-event nil 1 {}) (sut/queue-event {} 1 {}) (sut/queue-event {} 1 nil))
      "Empty events are not queued")
  (is (= #::sim-rc{:queue [#::sim-rc{:seizing-event #::sim-engine{:type :a
                                                                  :bucket 1}
                                     :consumed-quantity 13}]
                   :resource-name ::test}
         (sut/queue-event #::sim-rc{:resource-name ::test}
                          13
                          #::sim-engine{:type :a
                                        :bucket 1}))
      "Queuing an event in an empty queue.")
  (is (= 3
         (-> (sut/queue-event #::sim-rc{:resource-name ::test
                                        :queue [{} {}]}
                              13
                              #::sim-engine{:type :a
                                            :bucket 1})
             ::sim-rc/queue
             count))
      "Further events are queued")
  (testing "Non stricty positive `consumed-quantity` are ignored"
    (are [x] (= 2
                (-> x
                    ::sim-rc/queue
                    count))
     (sut/queue-event #::sim-rc{:resource-name ::test
                                :queue [{} {}]}
                      0
                      #::sim-engine{:type :a
                                    :bucket 1})
     (sut/queue-event #::sim-rc{:resource-name ::test
                                :queue [{} {}]}
                      -1
                      #::sim-engine{:type :a
                                    :bucket 1})
     (sut/queue-event #::sim-rc{:resource-name ::test
                                :queue [{} {}]}
                      ""
                      #::sim-engine{:type :a
                                    :bucket 1})
     (sut/queue-event #::sim-rc{:resource-name ::test
                                :queue [{} {}]}
                      nil
                      #::sim-engine{:type :a
                                    :bucket 1}))))

(deftest unqueue-event-test
  (is (= [[] #::sim-rc{:queue []}] (sut/unqueue-event nil 1 sim-rc-unblocking-policy/fifo))
      "Unqueue empty queue is noop")
  (is
   (= [[] #::sim-rc{:queue [{:a 2}]}]
      (sut/unqueue-event #::sim-rc{:queue [{:a 2}]} 0 sim-rc-unblocking-policy/fifo))
   "If unqueue is triggered with a zero or negative availability, it is not modifying the resource")
  (is (= [[#::sim-rc{:seizing-event #::sim-engine{:type :a
                                                  :bucket 1}
                     :consumed-quantity 2}]
          #::sim-rc{:queue [#::sim-rc{:seizing-event #::sim-engine{:type :b
                                                                   :bucket 2}
                                      :consumed-quantity 2}]}]
         (sut/unqueue-event #::sim-rc{:queue [#::sim-rc{:seizing-event #::sim-engine{:type :a
                                                                                     :bucket 1}
                                                        :consumed-quantity 2}
                                              #::sim-rc{:seizing-event #::sim-engine{:type :b
                                                                                     :bucket 2}
                                                        :consumed-quantity 2}]}
                            2
                            sim-rc-unblocking-policy/fifo))
      "First met event in the queue is released.")
  (is (= [[#::sim-rc{:seizing-event #::sim-engine{:type :a
                                                  :bucket 1}}]
          #::sim-rc{:queue [#::sim-rc{:seizing-event #::sim-engine{:type :b
                                                                   :bucket 2}}]}]
         (sut/unqueue-event #::sim-rc{:queue [#::sim-rc{:seizing-event #::sim-engine{:type :a
                                                                                     :bucket 1}}
                                              #::sim-rc{:seizing-event #::sim-engine{:type :b
                                                                                     :bucket 2}}]}
                            1
                            sim-rc-unblocking-policy/fifo))
      "First met event in the queue is released. The `consumed-quantity` is defaulted to 1.")
  (is (= [[]
          #::sim-rc{:queue [#::sim-rc{:seizing-event #::sim-engine{:type :a
                                                                   :bucket 1}
                                      :consumed-quantity 3}
                            #::sim-rc{:seizing-event #::sim-engine{:type :b
                                                                   :bucket 2}}]}]
         (sut/unqueue-event #::sim-rc{:queue [#::sim-rc{:seizing-event #::sim-engine{:type :a
                                                                                     :bucket 1}
                                                        :consumed-quantity 3}
                                              #::sim-rc{:seizing-event #::sim-engine{:type :b
                                                                                     :bucket 2}}]}
                            1
                            sim-rc-unblocking-policy/fifo))
      "Unqueue is blocked until the first element according to the policy could be released")
  (is (= [[#::sim-rc{:seizing-event #::sim-engine{:type :a
                                                  :bucket 0}}]
          {::sim-rc/queue []}]
         (sut/unqueue-event #::sim-rc{:queue [#::sim-rc{:seizing-event #::sim-engine{:type :a
                                                                                     :bucket 0}}]}
                            1
                            sim-rc-unblocking-policy/fifo))
      "Unqueue can drop the last element and returns an empty queue")
  (is (= [[#::sim-rc{:seizing-event {:event :a}
                     :consumed-quantity 1}]
          #::sim-rc{:queue []}]
         (-> {}
             (sut/queue-event 1 {:event :a})
             (sut/unqueue-event 1 sim-rc-unblocking-policy/fifo)))
      "Unqueue returns the element if the quantity")
  (is (= [[#::sim-rc{:seizing-event {:a :b}
                     :consumed-quantity 17}]
          #::sim-rc{:queue [#::sim-rc{:seizing-event {:a :c}
                                      :consumed-quantity 19}
                            #::sim-rc{:seizing-event {:d :b}
                                      :consumed-quantity 11}]}]
         (-> {}
             (sut/queue-event 17 {:a :b})
             (sut/queue-event 19 {:a :c})
             (sut/queue-event 11 {:d :b})
             (sut/unqueue-event 17 sim-rc-unblocking-policy/fifo)))
      "Unqueue is exactly matching the first element")
  (is (= [[#::sim-rc{:seizing-event {:a :b}
                     :consumed-quantity 1}
           #::sim-rc{:seizing-event {:d :b}
                     :consumed-quantity 2}]
          #::sim-rc{:queue []}]
         (-> {}
             (sut/queue-event 1 {:a :b})
             (sut/queue-event 2 {:d :b})
             (sut/unqueue-event 5 sim-rc-unblocking-policy/fifo)))
      "Unqueue more than what is seized by the events")
  (is (= [[]
          #::sim-rc{:queue [#::sim-rc{:seizing-event {:a :b}
                                      :consumed-quantity 10}
                            #::sim-rc{:seizing-event {:d :b}
                                      :consumed-quantity 20}]}]
         (-> {}
             (sut/queue-event 10 {:a :b})
             (sut/queue-event 20 {:d :b})
             (sut/unqueue-event 5 sim-rc-unblocking-policy/fifo)))
      "No event is released if available capacity is less than what is seized")
  (testing "unqueue exactly the expected capacity of two events"
    (is (= [[#::sim-rc{:seizing-event {:a :b}
                       :consumed-quantity 1}
             #::sim-rc{:seizing-event {:d :b}
                       :consumed-quantity 2}]
            #::sim-rc{:queue []}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:d :b})
               (sut/unqueue-event 3 sim-rc-unblocking-policy/fifo))))
    (is (= [[#::sim-rc{:seizing-event {:a :b}
                       :consumed-quantity 1}]
            #::sim-rc{:queue [#::sim-rc{:seizing-event {:d :b}
                                        :consumed-quantity 2}]}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:d :b})
               (sut/unqueue-event 2 sim-rc-unblocking-policy/fifo))))
    (is (= [[#::sim-rc{:seizing-event {:a :b}
                       :consumed-quantity 1}
             #::sim-rc{:seizing-event {:d :b}
                       :consumed-quantity 2}]
            #::sim-rc{:queue []}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:d :b})
               (sut/unqueue-event 4 sim-rc-unblocking-policy/fifo))))
    (is (= [[#::sim-rc{:seizing-event {:a :b}
                       :consumed-quantity 1}
             #::sim-rc{:seizing-event {:b :b}
                       :consumed-quantity 2}]
            #::sim-rc{:queue [#::sim-rc{:seizing-event {:c :b}
                                        :consumed-quantity 4}
                              #::sim-rc{:seizing-event {:d :b}
                                        :consumed-quantity 2}]}]
           (-> {}
               (sut/queue-event 1 {:a :b})
               (sut/queue-event 2 {:b :b})
               (sut/queue-event 4 {:c :b})
               (sut/queue-event 2 {:d :b})
               (sut/unqueue-event 4 sim-rc-unblocking-policy/fifo))))))
