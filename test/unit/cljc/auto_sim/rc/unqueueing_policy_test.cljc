(ns auto-sim.rc.unqueueing-policy-test
  (:require
   [auto-sim.rc                   :as-alias sim-rc]
   [auto-sim.rc.unqueueing-policy :as sut]
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])))

(deftest fifo-test (is (= [:a [:b :c]] (sut/fifo [:a :b :c]))))

(deftest lifo-test (is (= [:c [:a :b]] (sut/lifo [:a :b :c]))))
