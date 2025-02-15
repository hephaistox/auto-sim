(ns auto-sim.entity
  "An entity is a part of the model living in the `state` with its lifecycle managed."
  (:refer-clojure :exclude [update])
  (:require
   [auto-core.uuid  :as uuid-gen]
   [auto-sim.engine :as-alias sim-engine]))

(defn create-id [] (uuid-gen/time-based-uuid))

(defn create
  "Creates an entity called `entity-id` with `entity-data`. The lifecycle of this entity starts at `bucket`.

  An error is documented if the entity is created already."
  [state bucket entity-name entity-id entity-data]
  (-> state
      (update-in [::entity entity-id]
                 (fn [entity]
                   (if (some? (::created entity))
                     (clojure.core/update entity
                                          ::errors
                                          (fnil conj [])
                                          #::{:why ::already-created
                                              :entity-id entity-id
                                              :entity-name entity-name
                                              :entity-state entity-data
                                              :bucket bucket})
                     {::created bucket
                      ::living bucket
                      ::entity-state entity-data})))))

(defn assign-event "Assign `event`" [entity-id event] (assoc event ::entity-id entity-id))

(defn errors
  "Returns a map associating all `entity` with an error to the list of errors."
  [{::keys [entity]
    :as _state}]
  (->> entity
       (mapv (fn [[entity-id entity]] (when-let [errors (get entity ::errors)] [entity-id errors])))
       (filter (comp not empty? second))
       (into {})))

(defn entity-errors
  "Returns `nil` if entity `entity-id` has no error,
  Or a sequence of errors"
  [state
   {::keys [entity-id]
    :as _event}]
  (get-in state [::entity entity-id ::errors]))

(defn update
  "Returns `state` updated with `f` applied to entity `entity-id`.
  `f` is applied with `(apply f entity-state args)`
  `bucket` is used to tag when errors occur and mark when the last updated has been done"
  [state bucket event f & args]
  (let [{::keys [entity-id]} event]
    (try
      (update-in
       state
       [::entity entity-id]
       (fn [old-entity]
         (let [updated-entity (-> old-entity
                                  (assoc ::living bucket)
                                  (clojure.core/update ::entity-state (partial apply f) args))]
           (cond
             (::disposed old-entity) (clojure.core/update updated-entity
                                                          ::errors
                                                          (fnil conj [])
                                                          #::{:args args
                                                              :bucket bucket
                                                              :entity-id entity-id
                                                              :function f
                                                              :old-entity old-entity
                                                              :why ::updating-a-disposed-entity})
             (not (::created old-entity)) (-> updated-entity
                                              (assoc ::created bucket)
                                              (clojure.core/update
                                               ::errors
                                               (fnil conj [])
                                               #::{:args args
                                                   :bucket bucket
                                                   :entity-id entity-id
                                                   :function f
                                                   :old-entity old-entity
                                                   :why ::updating-a-not-created-entity}))
             :else updated-entity))))
      (catch #?(:clj Exception
                :cljs :default)
        e
        (-> state
            (update-in [::entity entity-id ::errors]
                       (fnil conj [])
                       #::{:args args
                           :bucket bucket
                           :entity-id entity-id
                           :exception e
                           :function f
                           :old-entity (get-in state [::entity entity-id])
                           :why ::exception-during-update}))))))

(defn state
  "Returns the `state` value of the entity called `entity-id`."
  [state
   {::keys [entity-id]
    :as _event}]
  (get-in state [::entity entity-id ::entity-state]))

(defn dispose
  "Disposing an entity by its `entity-id` is removing its data, its lifecycle will mark `::disposed` at the current `bucket`."
  [state bucket event]
  (let [{::keys [entity-id]} event]
    (-> state
        (update-in [::entity entity-id]
                   (fn [old-entity]
                     (let [updated-entity (-> old-entity
                                              (assoc ::disposed bucket)
                                              (dissoc ::entity-state))]
                       (cond
                         (nil? (::created old-entity))
                         (-> updated-entity
                             (assoc ::created bucket)
                             (assoc ::living bucket)
                             (clojure.core/update ::errors
                                                  conj
                                                  #::{:bucket bucket
                                                      :entity-id entity-id
                                                      :old-entity old-entity
                                                      :why ::disposing-a-not-created-entity}))
                         (some? (::disposed old-entity)) (-> updated-entity
                                                             (clojure.core/update
                                                              ::errors
                                                              conj
                                                              #::{:bucket bucket
                                                                  :entity-id entity-id
                                                                  :old-entity old-entity
                                                                  :why ::already-disposed}))
                         :else updated-entity)))))))

(defn lifecycle-status
  "The lifecycle has three possible fields `::created`, `::living` or `:disposed` depending on the position of the entity in its lifecycle."
  [state
   {::keys [entity-id]
    :as _event}]
  (-> state
      (get-in [::entity entity-id])
      (select-keys [::created ::living ::disposed])))

(defn is-created?
  "Is the entity called `:entity-id` living?"
  [state
   {::keys [entity-id]
    :as _event}]
  (let [{::keys [created]} (get-in state [::entity entity-id])] created))

(defn is-living?
  "Is the entity called `:entity-id` living?"
  [state
   {::keys [entity-id]
    :as _event}]
  (let [{::keys [living disposed]} (get-in state [::entity entity-id])]
    (when (and living (not disposed)) living)))

(defn is-disposed?
  "Is the entity called `:entity-id` disposed?"
  [state
   {::keys [entity-id]
    :as _event}]
  (let [{::keys [disposed]} (get-in state [::entity entity-id])] disposed))
