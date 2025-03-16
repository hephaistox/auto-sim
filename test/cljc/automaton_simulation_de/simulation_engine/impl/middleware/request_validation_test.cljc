(ns auto-sim.simulation-engine.impl.middleware.request-validation-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.simulation-engine                                    :as-alias sim-engine]
   [auto-sim.simulation-engine.impl.middleware.request-validation :as sut]
   [auto-sim.simulation-engine.impl.stopping.cause                :as sim-de-stopping-cause]
   [automaton-core.adapters.schema                                :as core-schema]))

(def ^:private event-stub
  #:auto-sim.simulation-engine{:type :a
                               :date 1})

(deftest evaluates-test
  (is (= nil
         (-> {::sim-engine/current-event event-stub
              ::sim-engine/event-execution (constantly {})
              :auto-sim.simulation-engine/snapshot
              #:auto-sim.simulation-engine{:id 1
                                           :iteration 1
                                           :date 1
                                           :state {}
                                           :past-events []
                                           :future-events [event-stub
                                                           #:auto-sim.simulation-engine{:type :b
                                                                                        :date 2}]}
              ::sim-engine/sorting (constantly nil)
              ::sim-engine/stopping-causes []}
             sut/evaluates))
      "Well form request is not modifying the request.")
  (is (= nil (core-schema/validate-data-humanize sim-de-stopping-cause/schema (sut/evaluates nil))))
  (is (= nil
         (-> {::sim-engine/current-event event-stub
              ::sim-engine/event-execution (constantly {})
              :auto-sim.simulation-engine/snapshot
              #:auto-sim.simulation-engine{:id 1
                                           :iteration 1
                                           :date 1
                                           :state {}
                                           :past-events []
                                           :future-events [event-stub
                                                           #:auto-sim.simulation-engine{:type :b
                                                                                        :date 2}]}
              ::sim-engine/sorting (constantly nil)
              ::sim-engine/stopping-causes []}
             sut/evaluates))
      "Well form request is not modifying the request.")
  (is (= nil
         (core-schema/validate-data-humanize sim-de-stopping-cause/schema (sut/evaluates nil)))))
