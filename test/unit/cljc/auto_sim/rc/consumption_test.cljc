(ns auto-sim.rc.consumption-test
  (:require
   [auto-sim.rc             :as-alias sim-rc]
   [auto-sim.rc.consumption :as sut]
   #?(:clj [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer [deftest is testing] :include-macros true])))

(deftest consume-test
  (testing "If event is not valid, the consumption is skipped"
    (is (= [nil nil] (sut/consume nil nil nil)))
    (is (= [nil {}] (sut/consume {} nil nil)))
    (is (= [nil {:a :b}] (sut/consume {:a :b} 1 nil))))
  (testing "Seizing are stored with a uuid"
    (is (uuid? (-> (sut/consume {} 1 {:a :b})
                   first))
        "The uuid is returned as a first element in the vector")
    (is (uuid? (-> (sut/consume {} 1 {:a :b})
                   second
                   ::sim-rc/currently-consuming
                   ffirst))
        "Consume is returning a consumption uuid"))
  (is (= #::sim-rc{:seizing-event {:a :b}
                   :consumed-quantity 3}
         (let [[consumption-uuid resource] (sut/consume {} 3 {:a :b})]
           (get-in resource [::sim-rc/currently-consuming consumption-uuid])))
      "Seizing are storing event and its quantity"))

(deftest free-test
  (is (= #::sim-rc{:currently-consuming {}}
         (sut/free #::sim-rc{:currently-consuming {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                                   #::sim-rc{:event {:a :b}
                                                             :consumed-quantity 3}}}
                   #uuid "33497220-f844-11ee-9fa1-17acea14e9df"))
      "A removed event is not in the list anymore")
  (is (= #::sim-rc{:currently-consuming {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                         #::sim-rc{:event {:a :b}
                                                   :consumed-quantity 3}}}
         (sut/free #::sim-rc{:currently-consuming {#uuid "33497220-f844-11ee-9fa1-17acea14e9df"
                                                   #::sim-rc{:event {:a :b}
                                                             :consumed-quantity 3}}}
                   #uuid "33497220-f844-11ee-9fa1-17acea14e8ee"))
      "A non existing event skips the freeing.")
  (is (= {::sim-rc/currently-consuming {}}
         (sut/free {::sim-rc/currently-consuming nil} #uuid "33497220-f844-11ee-9fa1-17acea14e9df")
         (sut/free nil #uuid "33497220-f844-11ee-9fa1-17acea14e9df"))
      "Empty currently consuming is ok"))
