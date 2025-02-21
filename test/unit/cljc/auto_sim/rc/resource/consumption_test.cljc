(ns auto-sim.rc.resource.consumption-test
  (:require
   [auto-sim.engine                  :as-alias sim-engine]
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.rc.resource.consumption :as sut]))

(deftest consume-test
  (is (= {:resource {}
          :errors [#::sim-engine{:why :event-miss-entity-id
                                 :consumption-quantity nil}]}
         (sut/start nil nil nil :a))
      "An event with no entity-id is documented as an error")
  (is (= {:resource {}
          :errors [#::sim-engine{:why :consumption-quantity-wrong
                                 :resource nil
                                 :consumption-quantity nil}]}
         (sut/start nil {::sim-engine/entity-id :entity-uuuid} nil :a))
      "A wrong consumption quantity is documented as an error")
  (is
   (= {:consumption-uuid :uuid-stub
       :resource #:auto-sim.engine{:consumption {:uuid-stub
                                                 #:auto-sim.engine{:entity-id :entity-uuid
                                                                   :priority :a
                                                                   :consumption-quantity 1}}}}
      (let [{:keys [consumption-uuid resource]} (sut/start {}
                                                           #::sim-engine{:a :b
                                                                         :entity-id :entity-uuid}
                                                           1
                                                           :a)]
        {:consumption-uuid :uuid-stub
         :resource (update resource
                           ::sim-engine/consumption
                           update-keys
                           (fn [k] (if (= k consumption-uuid) :uuid-stub k)))}))
   "The consumption stores `entity-id` and `consumption-quantity` uder the generated `consumption-uuid`"))

(deftest free-test
  (is (= {:resource #::sim-engine{:consumption {}}}
         (sut/ended #::sim-engine{:consumption {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                                #::sim-engine{:entity-id :entity-uuid
                                                              :consumed-quantity 3}}}
                    #uuid "33497220-f844-11ee-9fa1-17acea14e9df"))
      "An event that has been successfully removed is not the consumption list anymore")
  (is (let [resource #::sim-engine{:consumption {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                                 #::sim-engine{:event {:a :b}
                                                               :consumed-quantity 3}}}]
        (= {:resource resource
            :errors [#::sim-engine{:why :consumption-uuid-does-not-exist
                                   :resource resource
                                   :consumption-uuid #uuid "33497220-f844-11ee-9fa1-17acea14e8ee"}]}
           (sut/ended resource #uuid "33497220-f844-11ee-9fa1-17acea14e8ee")))
      "A non existing consumption-uuid does not modify the  resource but returns an error"))

(deftest compare-by-order-test
  (is (neg? ((sut/compare-by-order [:a :b]) :a :b)) "a is before b -> negative value")
  (is (pos? ((sut/compare-by-order [:a :b]) :b :a)) "b is before a -> positive value")
  (is (zero? ((sut/compare-by-order [:a :b]) :b :b)) "b equals b -> zero"))

(deftest consumption-by-priority-test
  (is (= [#:auto-sim.engine{:entity-id :entity-uuid
                            :priority :a
                            :consumption-quantity 1}
          #:auto-sim.engine{:entity-id :entity-uuid
                            :priority :b
                            :consumption-quantity 2}]
         (mapv second
               (-> (sut/start {}
                              #::sim-engine{:a :b
                                            :entity-id :entity-uuid}
                              1
                              :a)
                   :resource
                   (sut/start #::sim-engine{:c :d
                                            :entity-id :entity-uuid}
                              2
                              :b)
                   :resource
                   (sut/consumption-by-priority (sut/compare-by-order [:a :b])))))
      "Returns `a` first as the order is describing")
  (is (= [#:auto-sim.engine{:entity-id :entity-uuid
                            :priority :b
                            :consumption-quantity 2}
          #:auto-sim.engine{:entity-id :entity-uuid
                            :priority :a
                            :consumption-quantity 1}]
         (mapv second
               (-> (sut/start {}
                              #::sim-engine{:a :b
                                            :entity-id :entity-uuid}
                              1
                              :a)
                   :resource
                   (sut/start #::sim-engine{:c :d
                                            :entity-id :entity-uuid}
                              2
                              :b)
                   :resource
                   (sut/consumption-by-priority (sut/compare-by-order [:b :a])))))
      "Returns `b` first as the order is describing"))
