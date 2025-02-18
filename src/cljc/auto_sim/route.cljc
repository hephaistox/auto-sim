(ns auto-sim.route
  "Routes are a predefined list of operations that entities will proceed.

  `routes` is based on entity to store the route to execute."
  (:refer-clojure :exclude [pop])
  (:require
   [auto-sim.engine :as sim-engine]
   [auto-sim.entity :as sim-entity]))

(defn entity-data
  "Returns data to store in the entity to be able to execute routes."
  [routes route-id]
  (let [route (get routes route-id)]
    #::sim-engine{:route-id route-id
                  :route route}))

(defn pop
  "Execute the next operation in the routes.

  Returns `event-return` with `state`"
  [event-return event bucket new-event]
  (let [{::sim-engine/keys [state]} event-return
        [current-operation rroute] (->> (sim-entity/state state event)
                                        ::sim-engine/route
                                        ((juxt first rest)))
        new-event (assoc new-event
                         ::sim-engine/bucket bucket
                         ::sim-engine/current-operation current-operation)]
    (-> event-return
        (assoc ::sim-engine/state
               (sim-entity/update state bucket event assoc ::sim-engine/route rroute))
        (sim-entity/schedule-same-entity event bucket new-event))))
