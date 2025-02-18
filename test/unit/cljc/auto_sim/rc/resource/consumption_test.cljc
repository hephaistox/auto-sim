(ns auto-sim.rc.resource.consumption-test
  (:require
   [auto-sim.engine                  :as-alias sim-engine]
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.rc.resource.consumption :as sut]))

(deftest consume-test
  (is (= {:resource {}
          :errors [#::sim-engine{:why :consume-has-no-event-id
                                 :event nil
                                 :resource nil
                                 :consumption-quantity nil}]}
         (sut/consume nil nil nil :a))
      "An event with no entity-id is documented as an error")
  (is (= {:resource {}
          :errors [#::sim-engine{:why :consumption-quantity-wrong
                                 :event #::sim-engine{:entity-id :entity-uuuid}
                                 :resource nil
                                 :consumption-quantity nil}]}
         (sut/consume nil {::sim-engine/entity-id :entity-uuuid} nil :a))
      "A wrong consumption quantity is documented as an error")
  (is (uuid? (-> (sut/consume {} #::sim-engine{:entity-id :entity-uuid} 1 :a)
                 :consumption-uuid))
      "The returned value is a vector starting with an uuid")
  (is
   (= #::sim-engine{:entity-id :entity-uuid
                    :priority :a
                    :consumption-quantity 1}
      (let [{:keys [consumption-uuid resource]} (sut/consume {}
                                                             #::sim-engine{:a :b
                                                                           :entity-id :entity-uuid}
                                                             1
                                                             :a)]
        (get-in resource [::sim-engine/consumption consumption-uuid])))
   "The consumption stores `entity-id` and `consumption-quantity` uder the generated `consumption-uuid`"))

(deftest free-test
  (is (= {:resource #::sim-engine{:consumption {}}}
         (sut/free #::sim-engine{:consumption {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
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
           (sut/free resource #uuid "33497220-f844-11ee-9fa1-17acea14e8ee")))
      "A non existing consumption-uuid does not modify the resource but returns an error"))

(deftest compare-by-order-test
  (is (neg? ((sut/compare-by-order [:a :b]) :a :b)) "a is before b -> negative value")
  (is (pos? ((sut/compare-by-order [:a :b]) :b :a)) "b is before a -> positive value")
  (is (zero? ((sut/compare-by-order [:a :b]) :b :b)) "b equals b -> zero"))

(deftest consumption-by-priority-test
  (is (= 1
         (-> (sut/consume {}
                          #::sim-engine{:a :b
                                        :entity-id :entity-uuid-1}
                          1
                          :a)
             :resource
             (sut/consume #::sim-engine{:c :d
                                        :entity-id :entity-uuid}
                          2
                          :b)
             :resource
             (sut/consumption-by-priority :entity-uuid (sut/compare-by-order [:a :b]))
             count))
      "Skip entities concerning other entities")
  (is (= [#:auto-sim.engine{:entity-id :entity-uuid
                            :priority :a
                            :consumption-quantity 1}
          #:auto-sim.engine{:entity-id :entity-uuid
                            :priority :b
                            :consumption-quantity 2}]
         (mapv second
               (-> (sut/consume {}
                                #::sim-engine{:a :b
                                              :entity-id :entity-uuid}
                                1
                                :a)
                   :resource
                   (sut/consume #::sim-engine{:c :d
                                              :entity-id :entity-uuid}
                                2
                                :b)
                   :resource
                   (sut/consumption-by-priority :entity-uuid (sut/compare-by-order [:a :b])))))
      "Returns `a` first as the order is describing")
  (is (= [#:auto-sim.engine{:entity-id :entity-uuid
                            :priority :b
                            :consumption-quantity 2}
          #:auto-sim.engine{:entity-id :entity-uuid
                            :priority :a
                            :consumption-quantity 1}]
         (mapv second
               (-> (sut/consume {}
                                #::sim-engine{:a :b
                                              :entity-id :entity-uuid}
                                1
                                :a)
                   :resource
                   (sut/consume #::sim-engine{:c :d
                                              :entity-id :entity-uuid}
                                2
                                :b)
                   :resource
                   (sut/consumption-by-priority :entity-uuid (sut/compare-by-order [:b :a])))))
      "Returns `b` first as the order is describing"))
