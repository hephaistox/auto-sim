(ns auto-sim.route
  "Routes are a predefined list of operations that entities will proceed.

  `routes` is based on entity to store the route to execute. It contains:
  * `route-id`
  * `route` the rest of route elements to be executed.
  * `current-operation` the operation currently executed"
  (:require
   [auto-sim.engine :as-alias sim-engine]
   [auto-sim.entity :as sim-entity]))

(defn entity-data
  "Returns the data to initialize an entity that will execute routes."
  [routes route-id]
  (let [route (get routes route-id)]
    #::sim-engine{:route-id route-id
                  :route route}))
(defn get-route
  "Returns the `route` for the current entity.

  Returns `nil` if none."
  [event-return event _]
  (let [{::sim-engine/keys [state]} event-return
        route (->> (sim-entity/state state event)
                   ::sim-engine/route)]
    (when-not (empty? route) route)))

(defn next-op
  "Update the entity data to the next operation in the `route`.
  * `current-operation` is moved to the next one.
  * `route` has the next one removed.

  Returns `event-return` with `route` updated in the entity"
  [event-return event bucket route]
  (let [{::sim-engine/keys [state]} event-return
        [current-operation rroute] ((juxt first (comp vec rest)) route)]
    (-> event-return
        (assoc ::sim-engine/state
               (sim-entity/update state
                                  event
                                  bucket
                                  assoc
                                  ::sim-engine/route rroute
                                  ::sim-engine/current-operation current-operation)))))

(defn current-operation
  "Returns the `current-operation` of the current entity - as defined in the `event`."
  [event-return event _bucket]
  (let [{::sim-engine/keys [state]} event-return]
    (-> (sim-entity/state state event)
        ::sim-engine/current-operation)))

(defn add-current-operation
  "Adds the `current-operation` to `new-event"
  [event-return event _bucket new-event]
  (let [{::sim-engine/keys [state]} event-return]
    (merge new-event
           (select-keys (sim-entity/state state event)
                        [::sim-engine/route-id ::sim-engine/current-operation]))))

(defn schedule
  "Schedules the execution of `new-event` at `bucket` with the same entity than `event`

  Returns `event-return` with `future-events` updated."
  [event-return event bucket new-event]
  (let [new-event (add-current-operation event-return event bucket new-event)]
    (sim-entity/schedule event-return event bucket new-event)))
