(ns auto-sim.simulation-engine.impl.built-in-sd.execution-not-found
  "`stopping-definition` to stop when the execution of an event is not found in the registry."
  (:require
   [auto-sim.simulation-engine          :as-alias sim-engine]
   [auto-sim.simulation-engine.response :as sim-de-response]))

(def stopping-definition
  #:auto-sim.simulation-engine{:id ::sim-engine/execution-not-found
                                              :next-possible? true
                                              :doc
                                              "Stops when the execution of an event is not found in the registry."})

(defn evaluates
  [response event]
  (->
    response
    (sim-de-response/add-stopping-cause
     #:auto-sim.simulation-engine{:stopping-criteria
                                                 #:auto-sim.simulation-engine{:stopping-definition
                                                                                             stopping-definition}
                                                 :current-event event
                                                 :context
                                                 #:auto-sim.simulation-engine{:not-found-type
                                                                                             (::sim-engine/type
                                                                                              event)}})))
