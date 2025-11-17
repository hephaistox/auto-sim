(ns auto-sim.rc.resource.queue-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.engine               :as-alias sim-engine]
   [auto-sim.rc.resource.queue    :as sut]
   [auto-sim.rc.unqueueing-policy :refer [fifo]]))

(deftest queue-event-test
  (is (= {:resource nil
          :errors [#::sim-engine{:why :queuing-an-empty-event
                                 :resource nil
                                 :consumption-quantity 1
                                 :priority :low
                                 :event {}}]}
         (sut/queue-event nil {} 1 :low))
      "Empty events are not queued")
  (is (= {:resource nil
          :errors [#::sim-engine{:why :consumption-quantity-wrong
                                 :resource nil
                                 :consumption-quantity -1
                                 :priority :low
                                 :event {::sim-engine/type :a}}]}
         (sut/queue-event nil {::sim-engine/type :a} -1 :low))
      "Wrong consumption-quantity are not queued")
  (is (= {:resource #::sim-engine{:queue [#::sim-engine{:event #::sim-engine{:type :a
                                                                             :bucket 1}
                                                        :priority :low
                                                        :consumption-quantity 13}]
                                  :resource-name ::test}}
         (sut/queue-event #::sim-engine{:resource-name ::test}
                          #::sim-engine{:type :a
                                        :bucket 1}
                          13
                          :low))
      "Successfully queuing an event in an empty queue.")
  (is (= 3
         (-> (sut/queue-event #::sim-engine{:resource-name ::test
                                            :queue [:stub1 :stub2]}
                              #::sim-engine{:type :a
                                            :bucket 1}
                              13
                              :low)
             :resource
             ::sim-engine/queue
             count))
      "Further events are queued"))

(deftest unqueue-event-test
  (is (= {:resource #::sim-engine{:queue []}} (sut/unqueue-event {::sim-engine/queue []} 1 fifo))
      "Unqueue an empty queue is noop")
  (is
   (= {:unqueued []
       :resource #::sim-engine{:queue [{:a 2}]}}
      (sut/unqueue-event #::sim-engine{:queue [{:a 2}]} 0 fifo))
   "If unqueue is triggered with a zero or negative availability, it is not modifying the resource")
  (is (= {:unqueued [#::sim-engine{:event #::sim-engine{:type :a
                                                        :bucket 1}
                                   :consumption-quantity 2}]
          :resource #::sim-engine{:queue [#::sim-engine{:event #::sim-engine{:type :b
                                                                             :bucket 2}
                                                        :consumption-quantity 2}]}}
         (sut/unqueue-event #::sim-engine{:queue [#::sim-engine{:event #::sim-engine{:type :a
                                                                                     :bucket 1}
                                                                :consumption-quantity 2}
                                                  #::sim-engine{:event #::sim-engine{:type :b
                                                                                     :bucket 2}
                                                                :consumption-quantity 2}]}
                            2
                            fifo))
      "With FIFO, the first met event in the queue is released.")
  (is (= {:unqueued []
          :resource #::sim-engine{:queue [#::sim-engine{:event #::sim-engine{:type :a
                                                                             :bucket 1}
                                                        :consumption-quantity 2}
                                          #::sim-engine{:event #::sim-engine{:type :b
                                                                             :bucket 2}
                                                        :consumption-quantity 2}]}}
         (sut/unqueue-event #::sim-engine{:queue [#::sim-engine{:event #::sim-engine{:type :a
                                                                                     :bucket 1}
                                                                :consumption-quantity 2}
                                                  #::sim-engine{:event #::sim-engine{:type :b
                                                                                     :bucket 2}
                                                                :consumption-quantity 2}]}
                            1
                            fifo))
      "If the released quantity is not enough, no event is returned")
  (is
   (= {:unqueued []
       :resource #::sim-engine{:queue [#::sim-engine{:event #::sim-engine{:type :a
                                                                          :bucket 1}
                                                     :consumption-quantity 2}
                                       #::sim-engine{:event #::sim-engine{:type :b
                                                                          :bucket 2}
                                                     :consumption-quantity 1}]}}
      (sut/unqueue-event #::sim-engine{:queue [#::sim-engine{:event #::sim-engine{:type :a
                                                                                  :bucket 1}
                                                             :consumption-quantity 2}
                                               #::sim-engine{:event #::sim-engine{:type :b
                                                                                  :bucket 2}
                                                             :consumption-quantity 1}]}
                         1
                         fifo))
   "If the released quantity is not enough, no event is returned. Even if a further event is matching, unqueuing policy is more important")
  (is (= {:unqueued [#::sim-engine{:event #::sim-engine{:type :a
                                                        :bucket 1}
                                   :consumption-quantity 2}
                     #::sim-engine{:event #::sim-engine{:type :b
                                                        :bucket 2}
                                   :consumption-quantity 1}]
          :resource #::sim-engine{:queue []}}
         (sut/unqueue-event #::sim-engine{:queue [#::sim-engine{:event #::sim-engine{:type :a
                                                                                     :bucket 1}
                                                                :consumption-quantity 2}
                                                  #::sim-engine{:event #::sim-engine{:type :b
                                                                                     :bucket 2}
                                                                :consumption-quantity 1}]}
                            100
                            fifo))
      "When quantity is big enough, all events are released.")
  (is (= {:unqueued [#::sim-engine{:event #::sim-engine{:type :a
                                                        :bucket 1}
                                   :consumption-quantity 2}
                     #::sim-engine{:event #::sim-engine{:type :b
                                                        :bucket 2}
                                   :consumption-quantity 1}]
          :resource #::sim-engine{:queue []}}
         (sut/unqueue-event #::sim-engine{:queue [#::sim-engine{:event #::sim-engine{:type :a
                                                                                     :bucket 1}
                                                                :consumption-quantity 2}
                                                  #::sim-engine{:event #::sim-engine{:type :b
                                                                                     :bucket 2}
                                                                :consumption-quantity 1}]}
                            3
                            fifo))
      "Available quantity is matching exactly the sum of released quantity")
  (is (= {:unqueued [#::sim-engine{:event #::sim-engine{:type :a
                                                        :bucket 0}}]
          :resource {::sim-engine/queue []}}
         (sut/unqueue-event #::sim-engine{:queue [#::sim-engine{:event #::sim-engine{:type :a
                                                                                     :bucket 0}}]}
                            1
                            fifo))
      "Unqueue can drop the last element and returns an empty queue"))


(deftest assembly
  (is (= {:unqueued [#::sim-engine{:event {:event :a}
                                   :priority :high
                                   :consumption-quantity 1}]
          :resource #::sim-engine{:queue []}}
         (-> (sut/queue-event {} {:event :a} 1 :high)
             :resource
             (sut/unqueue-event 1 fifo)))
      "Unqueue returns the element if the quantity is matching")
  (is (= {:unqueued [#::sim-engine{:event {:a :b}
                                   :priority :high
                                   :consumption-quantity 17}]
          :resource {::sim-engine/queue [#::sim-engine{:event {:a :c}
                                                       :priority :high
                                                       :consumption-quantity 19}
                                         #::sim-engine{:event {:d :b}
                                                       :priority :high
                                                       :consumption-quantity 11}]}}
         (-> {}
             (sut/queue-event {:a :b} 17 :high)
             :resource
             (sut/queue-event {:a :c} 19 :high)
             :resource
             (sut/queue-event {:d :b} 11 :high)
             :resource
             (sut/unqueue-event 17 fifo)))
      "Unqueue is exactly matching the first element")
  (is (= {:unqueued [#::sim-engine{:event {:a :b}
                                   :priority :high
                                   :consumption-quantity 1}
                     #::sim-engine{:event {:d :b}
                                   :priority :high
                                   :consumption-quantity 2}]
          :resource #::sim-engine{:queue []}}
         (-> {}
             (sut/queue-event {:a :b} 1 :high)
             :resource
             (sut/queue-event {:d :b} 2 :high)
             :resource
             (sut/unqueue-event 5 fifo)))
      "Unqueue all events")
  (is (= {:unqueued []
          :resource #::sim-engine{:queue [#::sim-engine{:event {:a :b}
                                                        :priority :high
                                                        :consumption-quantity 10}
                                          #::sim-engine{:event {:d :b}
                                                        :priority :high
                                                        :consumption-quantity 20}]}}
         (-> {}
             (sut/queue-event {:a :b} 10 :high)
             :resource
             (sut/queue-event {:d :b} 20 :high)
             :resource
             (sut/unqueue-event 5 fifo)))
      "No event is released if available capacity is less than what is seized")
  (is (= {:unqueued [#::sim-engine{:event {:a :b}
                                   :priority :high
                                   :consumption-quantity 1}
                     #::sim-engine{:event {:d :b}
                                   :priority :high
                                   :consumption-quantity 2}]
          :resource #::sim-engine{:queue []}}
         (-> {}
             (sut/queue-event {:a :b} 1 :high)
             :resource
             (sut/queue-event {:d :b} 2 :high)
             :resource
             (sut/unqueue-event 3 fifo)))
      "unqueue exactly the expected capacity of two events"))
