(ns auto-sim.simulation-engine.impl.middleware.response-validation
  "Stops when the response is valid through inconsistency and schema.
  This criteria is built-in to this middleware as it is requiring the `response` knowledge. User `stopping-criteria` knows only `snapshot`."
  (:require
   [auto-sim.simulation-engine                                      :as-alias sim-engine]
   [auto-sim.simulation-engine.impl.built-in-sd.response-validation :as sim-de-response-validation]
   [auto-sim.simulation-engine.response                             :as sim-de-response]
   [auto-sim.simulation-engine.snapshot                             :as sim-de-snapshot]
   [automaton-core.adapters.schema                                  :as core-schema]))

(defn evaluates
  [{::sim-engine/keys [snapshot]
    :as response}
   current-event]
  (let [response-inconsistency (sim-de-snapshot/inconsistency? snapshot)
        response-inconsistency? (not (false? response-inconsistency))
        response-schema-error (core-schema/validate-data-humanize sim-de-response/schema response)
        response-error? (some? response-schema-error)]
    (when (or response-inconsistency? response-error?)
      (cond->
        #:auto-sim.simulation-engine{:stopping-criteria
                                     #:auto-sim.simulation-engine{:stopping-definition
                                                                  (sim-de-response-validation/stopping-definition)}
                                     :current-event current-event
                                     :context #:auto-sim.simulation-engine{:response response}}
        response-inconsistency? (assoc-in [::sim-engine/context ::sim-engine/inconsistency]
                                 response-inconsistency)
        response-error? (assoc-in [::sim-engine/context ::sim-engine/schema]
                         response-schema-error)))))

(defn wrap-response
  [handler]
  (fn [{::sim-engine/keys [current-event]
        :as request}]
    (let [response (handler request)]
      (sim-de-response/add-stopping-cause response (evaluates response current-event)))))
