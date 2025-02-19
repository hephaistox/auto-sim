(ns auto-sim.entity
  "An entity is a part of the model living in the `state` with its lifecycle managed.

  Entities are identified with `uuid` automatically generated."
  (:refer-clojure :exclude [update])
  (:require
   [auto-core.uuid  :as uuid-gen]
   [auto-sim.engine :as-alias sim-engine]))

;; ********************************************************************************
;; Internal API
;; ********************************************************************************

(defn- create-id [] (uuid-gen/time-based-uuid))

(defn create
  "Creates an entity called `entity-id` with `entity-data`. The lifecycle of this entity starts at `bucket`.

  An error is documented if the entity is created already.

  Returns `state` updated."
  [state bucket entity-name entity-id entity-data]
  (-> state
      (update-in [::sim-engine/entity entity-id]
                 (fn [entity]
                   (if (some? (::sim-engine/created entity))
                     (clojure.core/update entity
                                          ::sim-engine/errors
                                          (fnil conj [])
                                          #::sim-engine{:why ::sim-engine/already-created
                                                        :entity-id entity-id
                                                        :entity-name entity-name
                                                        :entity-state entity-data
                                                        :bucket bucket})
                     #::sim-engine{:created bucket
                                   :living bucket
                                   :entity-state entity-data})))))

(defn assign-event
  "Assign `entity-id` to `event`."
  [entity-id event]
  (assoc event ::sim-engine/entity-id entity-id))

(defn errors
  "Return `entity` with at least one error."
  [{::sim-engine/keys [entity]
    :as _state}]
  (->> entity
       (mapv (fn [[entity-id entity]]
               (when-let [errors (get entity ::sim-engine/errors)] [entity-id errors])))
       (filter (comp not empty? second))
       (into {})))

(defn entity-errors
  "Return `errors` from current entity in `event`.
  Returns `nil` otherwise."
  [state
   {::sim-engine/keys [entity-id]
    :as _event}]
  (get-in state [::sim-engine/entity entity-id ::sim-engine/errors]))

(defn update
  "Returns `state` updated with `f` applied to entity `entity-id`.
  `f` is applied with `args`: `(apply f entity-state args)`

  `bucket` is used to tag when errors occur and mark when the last updated has been done"
  [state event bucket f & args]
  (let [{::sim-engine/keys [entity-id]} event]
    (try
      (update-in
       state
       [::sim-engine/entity entity-id]
       (fn [old-entity]
         (let [updated-entity
               (-> old-entity
                   (assoc ::sim-engine/living bucket)
                   (clojure.core/update ::sim-engine/entity-state (partial apply f) args))]
           (cond
             (::sim-engine/disposed old-entity)
             (clojure.core/update updated-entity
                                  ::sim-engine/errors
                                  (fnil conj [])
                                  #::sim-engine{:args args
                                                :bucket bucket
                                                :entity-id entity-id
                                                :function f
                                                :old-entity old-entity
                                                :why ::sim-engine/updating-a-disposed-entity})
             (not (::sim-engine/created old-entity))
             (-> updated-entity
                 (assoc ::sim-engine/created bucket)
                 (clojure.core/update ::sim-engine/errors
                                      (fnil conj [])
                                      #::sim-engine{:args args
                                                    :bucket bucket
                                                    :entity-id entity-id
                                                    :function f
                                                    :old-entity old-entity
                                                    :why
                                                    ::sim-engine/updating-a-not-created-entity}))
             :else updated-entity))))
      (catch #?(:clj Exception
                :cljs :default)
        e
        (-> state
            (update-in [::sim-engine/entity entity-id ::sim-engine/errors]
                       (fnil conj [])
                       #::sim-engine{:args args
                                     :bucket bucket
                                     :entity-id entity-id
                                     :exception e
                                     :function f
                                     :old-entity (get-in state [::sim-engine/entity entity-id])
                                     :why ::sim-engine/exception-during-update}))))))

(defn state
  "Returns the `state` value of the entity called `entity-id`."
  [state
   {::sim-engine/keys [entity-id]
    :as _event}]
  (get-in state [::sim-engine/entity entity-id ::sim-engine/entity-state]))

(defn- dispose*
  [state bucket event dissoc-state?]
  (let [{::sim-engine/keys [entity-id]} event]
    (-> state
        (update-in [::sim-engine/entity entity-id]
                   (fn [old-entity]
                     (let [updated-entity (cond-> old-entity
                                            true (assoc ::sim-engine/disposed bucket)
                                            dissoc-state? (dissoc ::sim-engine/entity-state))]
                       (cond
                         (nil? (::sim-engine/created old-entity))
                         (-> updated-entity
                             (assoc ::sim-engine/created bucket)
                             (assoc ::sim-engine/living bucket)
                             (clojure.core/update
                              ::sim-engine/errors
                              (fnil conj [])
                              #::sim-engine{:bucket bucket
                                            :entity-id entity-id
                                            :old-entity old-entity
                                            :why ::sim-engine/disposing-a-not-created-entity}))
                         (some? (::sim-engine/disposed old-entity))
                         (-> updated-entity
                             ;;TODO Move to event-return
                             (clojure.core/update ::sim-engine/errors
                                                  (fnil conj [])
                                                  #::sim-engine{:bucket bucket
                                                                :entity-id entity-id
                                                                :old-entity old-entity
                                                                :why
                                                                ::sim-engine/already-disposed}))
                         :else updated-entity)))))))

(defn dispose
  "Disposing an entity by its `entity-id` is removing its data, its lifecycle will mark `::sim-engine/disposed` at the current `bucket`."
  [state event bucket]
  (dispose* state bucket event true))

(defn dispose-with-history
  "Disposing an entity by its `entity-id` is removing its data, its lifecycle will mark `::sim-engine/disposed` at the current `bucket`."
  [state event bucket]
  (dispose* state bucket event false))

(defn lifecycle-status
  "The lifecycle has three possible fields `::sim-engine/created`, `::sim-engine/living` or `::sim-engine/disposed` depending on the position of the entity in its lifecycle."
  [state
   {::sim-engine/keys [entity-id]
    :as _event}]
  (-> state
      (get-in [::sim-engine/entity entity-id])
      (select-keys [::sim-engine/created ::sim-engine/living ::sim-engine/disposed])))

(defn is-created?
  "Is the entity called `:entity-id` living?"
  [state
   {::sim-engine/keys [entity-id]
    :as _event}]
  (let [{::sim-engine/keys [created]} (get-in state [::sim-engine/entity entity-id])] created))

(defn is-living?
  "Is the entity called `:entity-id` living?"
  [state
   {::sim-engine/keys [entity-id]
    :as _event}]
  (let [{::sim-engine/keys [living disposed]} (get-in state [::sim-engine/entity entity-id])]
    (when (and living (not disposed)) living)))

(defn is-disposed?
  "Is the entity called `:entity-id` disposed?"
  [state
   {::sim-engine/keys [entity-id]
    :as _event}]
  (let [{::sim-engine/keys [disposed]} (get-in state [::sim-engine/entity entity-id])] disposed))

(defn n-entities-event
  "Event to create `n` entities separated with `waiting-time` buckets"
  [bucket n waiting-time type]
  {::sim-engine/type type
   ::sim-engine/bucket bucket
   ::waiting-time waiting-time
   ::nb-entity 0
   ::max-nb-entity n})

;; ********************************************************************************
;; Internal API
;; ********************************************************************************

(defn schedule-entity-every
  "Schedule an entity creation every `waiting-time` buckets.

  The event is created only `max-nb-entity` times

  Returns an event-return with:
  * `::sim-engine/state` with a new entity created named `entity-name`
  * `::sim-engine/future-events` with events to create next entities in `waiting-time` buckets"
  [event-return event bucket entity-name entity-data]
  (let [{::sim-engine/keys [state future-events]} event-return
        entity-id (create-id)
        {::keys [nb-entity max-nb-entity waiting-time]} event
        nb-entity (inc nb-entity)]
    (assoc event-return
           ::sim-engine/entity-id entity-id
           ::sim-engine/state (create state bucket entity-name entity-id entity-data)
           ::sim-engine/future-events (cond-> future-events
                                        (< nb-entity max-nb-entity)
                                        (conj (assoc event
                                                     ::sim-engine/bucket (+ bucket waiting-time)
                                                     ::nb-entity nb-entity))))))

(defn schedule*
  [event-return _event _bucket new-event entity-id bucket]
  (if (nil? entity-id)
    (clojure.core/update event-return
                         ::sim-engine/errors
                         (fnil conj [])
                         #::sim-engine{:why :no-entity-id-to-pick
                                       :entity-id entity-id
                                       :event-return event-return})
    (let [scheduled-event (-> new-event
                              (assoc ::sim-engine/entity-id entity-id ::sim-engine/bucket bucket))]
      (-> event-return
          (clojure.core/update ::sim-engine/future-events (fnil conj []) scheduled-event)))))

(defn schedule-new-entity
  "Schedules the execution of `new-event` at `bucket` for the entity stored in `event-return ` by `schedule-entity-every` will be used.

  If none exist, an error is raised.

  Returns `event-return` with `future-events` updated."
  [event-return event bucket new-event]
  (let [event-id (::sim-engine/entity-id event-return)]
    (schedule* event-return event bucket new-event event-id bucket)))

(defn schedule
  "Schedules the execution of `new-event` at `bucket` for the same entity than `event`

  Returns `event-return` with `future-events` updated."
  [event-return event bucket new-event]
  (schedule* event-return event bucket new-event (::sim-engine/entity-id event) bucket))

(defn sink
  "End of life of an entity"
  [event-return event bucket]
  (-> event-return
      (clojure.core/update ::sim-engine/state dispose* bucket event false)))
