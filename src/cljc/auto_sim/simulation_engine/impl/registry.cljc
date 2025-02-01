(ns auto-sim.simulation-engine.impl.registry
  "Gathers the registries of a simulation."
  (:require
   [auto-sim.simulation-engine                                   :as-alias
                                                                                sim-engine]
   [auto-sim.simulation-engine.impl.event-registry
    :as sim-de-event-registry]
   [auto-sim.simulation-engine.impl.middleware.registry
    :as sim-de-middleware-registry]
   [auto-sim.simulation-engine.impl.ordering.registry
    :as sim-de-ordering-registry]
   [auto-sim.simulation-engine.impl.stopping-definition.registry
    :as sim-de-stopping-registry]))

(def schema
  [:map {:closed true}
   [::sim-engine/stopping sim-de-stopping-registry/schema]
   [::sim-engine/middleware sim-de-middleware-registry/schema]
   [::sim-engine/event sim-de-event-registry/schema]
   [::sim-engine/ordering sim-de-ordering-registry/schema]])

(defn build
  []
  #:auto-sim.simulation-engine{:stopping (sim-de-stopping-registry/build)
                                              :middleware (sim-de-middleware-registry/build)
                                              :event {}
                                              :ordering (sim-de-ordering-registry/build)})
