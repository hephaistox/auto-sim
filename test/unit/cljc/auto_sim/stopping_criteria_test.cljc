(ns auto-sim.stopping-criteria-test
  (:require
   [auto-sim.engine            :as-alias sim-engine]
   [auto-sim.stopping-criteria :as sut]
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])))

(deftest stop-bucket-sd-test
  (is (nil? ((sut/stop-bucket-sd 2) #::sim-engine{:bucket 1}))
      "A bucket in the future doesn't stop")
  (is (= #::sim-engine{:doc ["Stops at bucket %d as required" 2]
                       :id ::sut/bucket-stopping
                       :context {:stopping-bucket 2
                                 :snapshot-bucket 2}}
         ((sut/stop-bucket-sd 2) #::sim-engine{:bucket 2}))
      "When the bucket is reached, returns an error")
  (is (= #::sim-engine{:doc ["Stops at bucket %d (requiring %d)" 3 2]
                       :id ::sut/bucket-stopping
                       :context {:stopping-bucket 2
                                 :snapshot-bucket 3}}
         ((sut/stop-bucket-sd 2) #::sim-engine{:bucket 3}))
      "When a bucket is exceeded, returned an error"))

(deftest stop-now-sd-test
  (is (= #::sim-engine{:doc ["Stop now."]
                       :id ::sut/stop-now
                       :context nil}
         ((sut/stop-now-sd) {}))))

(deftest stop-state-contains-sd-test
  (is (nil? ((sut/stop-state-contains-sd #::sim-engine{:state-path [:foo :bar]}) {:foo {}}))
      "If the path is not present, returns no error.")
  (is (= #::sim-engine{:doc ["Stops as state contains `%s` in `%s`" true [:foo :bar]]
                       :id ::sut/state-contains
                       :context #::sim-engine{:snapshot-state {:foo {:bar true}}
                                              :state-entry true}}
         ((sut/stop-state-contains-sd [:foo :bar]) #::sim-engine{:state {:foo {:bar true}}}))))

(deftest eval-test
  (is (not-empty (sut/eval [(sut/stop-now-sd)] {}))
      "A stopping criteria stops the whole evaluation")
  (is (= [] (sut/eval [(sut/stop-state-contains-sd #::sim-engine{:state-path [:foo]})] {}))
      "A criteria which is not matched")
  (is (= [#::sim-engine{:doc ["Stop now."]
                        :id ::sut/stop-now
                        :context nil}]
         (sut/eval [(sut/stop-state-contains-sd #::sim-engine{:state-path [:foo]})
                    (sut/stop-now-sd)]
                   {}))
      "A criteria which is not matched"))

(deftest stop-bucket-test (sut/stop-bucket {} 12))
