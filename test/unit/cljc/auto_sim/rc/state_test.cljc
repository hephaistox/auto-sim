(ns auto-sim.rc.state-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.engine               :as-alias sim-engine]
   [auto-sim.rc                   :as-alias sim-rc]
   [auto-sim.rc.preemption-policy :refer [no-preemption]]
   [auto-sim.rc.state             :as sut]
   [auto-sim.rc.unblocking-policy :refer [fifo]]))

(defn- keys->sequence-number
  "Return a map associating a key of the map `m` with a number, numbered from 1 to n."
  [m]
  (zipmap (keys (into {} m)) (iterate inc 1)))

(defn- translate-keys
  "Translate keys of map `m` thanks to the `translation`."
  [m translation]
  (->> m
       (map (fn [[k v]] [(get translation k k) v]))
       (into {})))


(defn- remove-consumption-uuid
  "To remove randomness and ease testing"
  [state resource-name]
  (let [translation (-> state
                        (get-in [::sim-rc/resource resource-name ::sim-rc/currently-consuming])
                        keys->sequence-number)]
    (-> state
        (update-in [::sim-rc/resource resource-name ::sim-rc/currently-consuming]
                   (fn [m] (translate-keys m translation))))))

(deftest define-resources-test
  (is (= #::sim-rc{:resource {}} (sut/define-resources {} {}) (sut/define-resources {} nil))
      "No resource is ok")
  (is (= #::sim-rc{:resource {::test #::sim-rc{:capacity 1}}}
         (sut/define-resources {::sim-rc/resource {::test #::sim-rc{:capacity 1}}} {}))
      "Existing resources are not modified")
  (is (= #::sim-rc{:resource {::test #::sim-rc{:capacity 1
                                               :name ::test
                                               :queue []
                                               :currently-consuming {}
                                               :renewable? true}}}
         (sut/define-resources {} {::test #::sim-rc{:capacity 1}}))
      "A resource is updated"))

(deftest update-resource-capacity-test
  (is (= [[]
          #::sim-rc{:resource {::test #::sim-rc{:capacity 12
                                                :queue []}}}]
         (sut/update-resource-capacity {} ::test 12 no-preemption fifo))
      "Empty resource is updated")
  (is (= [[]
          #::sim-rc{:resource {::test #::sim-rc{:capacity 17
                                                :queue []}}}]
         (sut/update-resource-capacity #::sim-rc{:resource {::test #::sim-rc{:capacity 12}}}
                                       ::test
                                       17
                                       no-preemption
                                       fifo))
      "Capacity is updated"))

(deftest seize-test
  (is (= [nil {:foo :bar}] (sut/seize {:foo :bar} ::test 1 nil)) "Nil event is skipped")
  (is (= [nil {:foo :bar}]
         (sut/seize {:foo :bar}
                    nil
                    1
                    #::sim-engine{:type ::a
                                  :date 4}))
      "Nil resource name is skipped")
  (is (= [nil {:foo :bar}]
         (sut/seize {:foo :bar}
                    ::not-existing-resource
                    1
                    #::sim-engine{:type ::a
                                  :date 4}))
      "Non existing resource name is skipped")
  (is
   (= #::sim-rc{:seizing-event #::sim-engine{:type ::a
                                             :date 5}
                :consumed-quantity 17}
      (let [[consumption-uuid resource] (-> #::sim-rc{:resource {::test #::sim-rc{:capacity 20}}}
                                            (sut/seize ::test
                                                       17
                                                       #::sim-engine{:type ::a
                                                                     :date 5}))]
        (get-in resource [::sim-rc/resource ::test ::sim-rc/currently-consuming consumption-uuid])))
   "When capacity is big enough, the seizing happens, so the consuming event is in the `currently-consuming`.")
  (is
   (= #::sim-rc{:resource {::test #::sim-rc{:currently-consuming
                                            {1 #::sim-rc{:seizing-event #::sim-engine{:type ::a
                                                                                      :date 5}
                                                         :consumed-quantity 9}
                                             2 #::sim-rc{:seizing-event #::sim-engine{:type ::b
                                                                                      :date 7}
                                                         :consumed-quantity 17}}
                                            :capacity 30}}}
      (-> #::sim-rc{:resource {::test #::sim-rc{:capacity 30}}}
          (sut/seize ::test
                     9
                     #::sim-engine{:type ::a
                                   :date 5})
          second
          (sut/seize ::test
                     17
                     #::sim-engine{:type ::b
                                   :date 7})
          second
          (remove-consumption-uuid ::test)))
   "Moves the postponed event in the ::currently-seizing - in the case another event is already there"))

(deftest dispose-capacity-test
  (is (= [[]
          {::sim-rc/resource {::a #::sim-rc{:currently-consuming {}
                                            :queue []}}}]
         (sut/dispose {}
                      ::a
                      #::sim-engine{:type :a
                                    :date 1}
                      fifo)
         (sut/dispose {::sim-rc/resource {::a {::sim-rc/currently-consuming {}}}} ::a 666 fifo))
      "Disposing unknown resource is working")
  (is (= [[]
          {::sim-rc/resource {::test #::sim-rc{:currently-consuming {}
                                               :queue []
                                               :capacity 30}}}]
         (-> #::sim-rc{:resource {::test #::sim-rc{:capacity 30}}}
             (sut/seize ::test
                        12
                        #::sim-engine{:type :a
                                      :date 1})
             second
             (remove-consumption-uuid ::test)
             (sut/dispose ::test 1 fifo)))
      "Disposing a currently consuming event is removed")
  (is (= [[]
          #::sim-rc{:resource {::test #::sim-rc{:currently-consuming
                                                {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                                 {:a :b}}}}}]
         (-> #::sim-rc{:resource {::test #::sim-rc{:currently-consuming
                                                   {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                                    {:a :b}}}}}
             (sut/dispose ::test #uuid "33497220-f844-11ee-9fa1-17acea14e9de" fifo)))
      "A non existing resource consumption uuid doesn't change currently seizing list")
  (is (= [[#::sim-rc{:seizing-event {:c :b}
                     :consumed-quantity 4}]
          #::sim-rc{:resource {::test #::sim-rc{:currently-consuming
                                                {2 #::sim-rc{:seizing-event {:b :b}
                                                             :consumed-quantity 9}}
                                                :queue [#::sim-rc{:seizing-event {:d :b}
                                                                  :consumed-quantity 5}]
                                                :capacity 17}}}]
         (-> #::sim-rc{:resource {::test #::sim-rc{:capacity 17}}}
             (sut/seize ::test 7 {:a :b})
             second
             (sut/seize ::test 9 {:b :b})
             second
             (sut/seize ::test 4 {:c :b})
             second
             (sut/seize ::test 5 {:d :b})
             second
             (remove-consumption-uuid ::test)
             (sut/dispose ::test 1 fifo)))
      "Disposing known resource is removed from list - case when others are still in the list"))
