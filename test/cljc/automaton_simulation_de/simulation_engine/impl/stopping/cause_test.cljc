(ns auto-sim.simulation-engine.impl.stopping.cause-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema                                                   :as
                                                                                     core-schema]
   [auto-sim.simulation-engine                                        :as-alias
                                                                                     sim-engine]
   [auto-sim.simulation-engine.impl.stopping-definition.iteration-nth
    :as sim-de-sc-iteration-nth]
   [auto-sim.simulation-engine.impl.stopping.cause                    :as sut]))

(deftest schema-test
  (is (= nil
         (-> sut/schema
             core-schema/validate-humanize)))
  (is
   (=
    nil
    (->
      sut/schema
      (core-schema/validate-data-humanize
       #:auto-sim.simulation-engine{:stopping-criteria
                                                   #:auto-sim.simulation-engine{:params
                                                                                               {:par1
                                                                                                :a}
                                                                                               :model-end?
                                                                                               true
                                                                                               :stopping-definition
                                                                                               #:auto-sim.simulation-engine{:id
                                                                                                                                           ::sim-engine/iteration-nth
                                                                                                                                           :doc
                                                                                                                                           "doc-test"
                                                                                                                                           :next-possible?
                                                                                                                                           true
                                                                                                                                           :stopping-evaluation
                                                                                                                                           sim-de-sc-iteration-nth/stop-nth}}
                                                   :current-event
                                                   #:auto-sim.simulation-engine{:type
                                                                                               :a
                                                                                               :date
                                                                                               1}
                                                   :context {}})))))
