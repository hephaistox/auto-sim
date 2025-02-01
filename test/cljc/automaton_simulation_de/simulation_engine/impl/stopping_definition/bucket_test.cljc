(ns auto-sim.simulation-engine.impl.stopping-definition.bucket-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema                                            :as core-schema]
   [auto-sim.simulation-engine                                 :as-alias sim-engine]
   [auto-sim.simulation-engine.impl.stopping-definition.bucket :as sut]
   [auto-sim.simulation-engine.impl.stopping.definition
    :as sim-de-sc-definition]))

(deftest stop-bucket-test
  (is (= #:auto-sim.simulation-engine{:stop? true
                                                     :context nil}
         (sut/stop-bucket nil nil))))

(deftest stopping-definition-test
  (is (= nil
         (->> (sut/stopping-definition)
              (core-schema/validate-data-humanize sim-de-sc-definition/schema)))))
