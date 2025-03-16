(ns auto-sim.simulation-engine.impl.built-in-sd.failed-event-execution
  "`stopping-definition` to stop when an execution has raised an exception."
  (:require
   [auto-sim.simulation-engine          :as-alias sim-engine]
   [auto-sim.simulation-engine.response :as sim-de-response]))

(def stopping-definition
  #:auto-sim.simulation-engine{:id :auto-sim.simulation-engine/failed-event-execution
                               :next-possible? true
                               :doc "Stops when an execution has raised an exception."})

(defn evaluates
  [response e current-event]
  (-> response
      (sim-de-response/add-stopping-cause
       #:auto-sim.simulation-engine{:stopping-criteria
                                    #:auto-sim.simulation-engine{:stopping-definition
                                                                 stopping-definition}
                                    :current-event current-event
                                    :context #:auto-sim.simulation-engine{:error e}})))
