(ns auto-sim.simulation-engine.impl.built-in-sd.no-future-events-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema                                              :as core-schema]
   [auto-sim.simulation-engine                                   :as-alias
                                                                                sim-engine]
   [auto-sim.simulation-engine.impl.built-in-sd.no-future-events :as sut]
   [auto-sim.simulation-engine.impl.stopping.definition
    :as sim-de-sc-definition]
   [auto-sim.simulation-engine.request                           :as
                                                                                sim-de-request]))

(def event-stub
  #:auto-sim.simulation-engine{:type :a
                                              :date 1})

(deftest stopping-definition-test
  (is (= nil
         (->> sut/stopping-definition
              (core-schema/validate-data-humanize sim-de-sc-definition/schema)))))

(deftest evaluates-test
  (is
   (=
    nil
    (core-schema/validate-data-humanize
     sim-de-request/schema
     (sut/evaluates
      #:auto-sim.simulation-engine{:current-event event-stub
                                                  :event-execution (constantly nil)
                                                  :snapshot
                                                  #:auto-sim.simulation-engine{:id 1
                                                                                              :iteration
                                                                                              1
                                                                                              :date
                                                                                              1
                                                                                              :state
                                                                                              {}
                                                                                              :past-events
                                                                                              []
                                                                                              :future-events
                                                                                              []}
                                                  :sorting (constantly nil)}
      []))))
  (is (= {:request true} (sut/evaluates {:request true} [event-stub]))
      "If `future-event` is not empty, don't return the no-future-events stopping-definition")
  (is (= ::sim-engine/no-future-events
         (-> (sut/evaluates nil [])
             ::sim-engine/stopping-causes
             first
             (get-in
              [::sim-engine/stopping-criteria ::sim-engine/stopping-definition ::sim-engine/id])))
      "If no `future-event` exists, returns the `stopping-cause`."))
