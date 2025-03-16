(ns auto-sim.simulation-engine.impl.built-in-sd.causality-broken-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.simulation-engine                                   :as-alias sim-engine]
   [auto-sim.simulation-engine.impl.built-in-sd.causality-broken :as sut]
   [auto-sim.simulation-engine.impl.stopping.cause               :as sim-de-stopping-cause]
   [auto-sim.simulation-engine.impl.stopping.definition          :as sim-de-sc-definition]
   [automaton-core.adapters.schema                               :as core-schema]))

(deftest stopping-definition-test
  (is (= nil
         (->> sut/stopping-definition
              (core-schema/validate-data-humanize sim-de-sc-definition/schema)))))

(def event-stub
  #:auto-sim.simulation-engine{:type :a
                               :date 1})

(deftest evaluates-test
  (is (= nil
         (sut/evaluates #:auto-sim.simulation-engine{:date 12}
                        #:auto-sim.simulation-engine{:date 12}
                        event-stub))
      "Same date snapshot are accepted.")
  (is (= nil
         (sut/evaluates #:auto-sim.simulation-engine{:date 12}
                        #:auto-sim.simulation-engine{:date 14}
                        event-stub))
      "Greater date snapshot are accepted.")
  (is (= nil
         (->> (sut/evaluates #:auto-sim.simulation-engine{:date 15}
                             #:auto-sim.simulation-engine{:date 12}
                             event-stub)
              (core-schema/validate-data-humanize sim-de-stopping-cause/schema)))
      "If next snapshot' date is smaller than current one, `causality-broken` criteria is raised."))
