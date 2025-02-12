(ns auto-sim.ordering-test
  (:require
   [auto-sim.engine   :as-alias sim-engine]
   [auto-sim.ordering :as sut]
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])))

(deftest fields-test
  (is (neg? ((sut/fields :foo) {:foo 1} {:foo 2})) "first is lower")
  (is (pos? ((sut/fields :foo) {:foo 1} {:foo 0})) "second is lower")
  (is (zero? ((sut/fields :foo) {:foo 2} {:foo 2})) "equality")
  (is (pos? ((sut/fields :foo) {} {:foo 2})) "first not defined is considered like infinity")
  (is (neg? ((sut/fields :foo) {:foo 1} {})) "second not defined is considered like infinity"))

(deftest types-test
  (is (neg? ((sut/types [:a :b]) {::sim-engine/type :a} {::sim-engine/type :b})) "first is lower")
  (is (pos? ((sut/types [:b :c]) {::sim-engine/type :c} {::sim-engine/type :b})) "second is lower")
  (is (zero? ((sut/types []) {::sim-engine/type :b} {::sim-engine/type :b})) "equality")
  (is (pos? ((sut/types [:a :b :c]) {} {::sim-engine/type :b}))
      "first not defined is considered like infinity")
  (is (neg? ((sut/types [:a :b :c]) {::sim-engine/type :a} {}))
      "second not defined is considered like infinity"))

(deftest hashes-test
  (is (= ((sut/hashes)
          {::sim-engine/type :a
           :foo 1}
          {::sim-engine/type :b
           :foo 2})
         (- ((sut/hashes)
             {::sim-engine/type :b
              :foo 2}
             {::sim-engine/type :a
              :foo 1})))
      "If hash says that a before b, so it says b is after a"))

(deftest sorter-test
  (is (= [{:auto-sim.engine/type :a
           :foo 1}
          {:auto-sim.engine/type :b
           :foo 2}]
         ((sut/sorter (sut/fields :foo) (sut/types [:a :b :c]))
          [{::sim-engine/type :a
            :foo 1}
           {::sim-engine/type :b
            :foo 2}])))
  (is (= [{:auto-sim.engine/type :a
           :foo 1}
          {:auto-sim.engine/type :b
           :foo 2}]
         ((sut/sorter (sut/fields :foo) (sut/types [:a :b :c]))
          [{::sim-engine/type :b
            :foo 2}
           {::sim-engine/type :a
            :foo 1}]))))
