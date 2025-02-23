(ns auto-sim.stopping-criteria
  (:refer-clojure :exclude [eval remove])
  (:require
   [auto-sim.engine :as-alias sim-engine]))

;; ********************************************************************************
;; Private API
;; ********************************************************************************

(defn add-to-stopping-definition
  [model f]
  (-> model
      (update ::sim-engine/stopping-definition (fnil conj []) f)))

(defn stop-bucket-sd
  [stopping-bucket]
  (fn [snapshot]
    (let [{snapshot-bucket ::sim-engine/bucket} snapshot]
      (when (or (nil? snapshot-bucket) (nil? stopping-bucket) (>= snapshot-bucket stopping-bucket))
        #::sim-engine{:doc (if (= stopping-bucket snapshot-bucket)
                             ["Stops at bucket %d as required" stopping-bucket]
                             ["Stops at bucket %d (requiring %d)" snapshot-bucket stopping-bucket])
                      :id ::bucket-stopping
                      :context {:stopping-bucket stopping-bucket
                                :snapshot-bucket snapshot-bucket}}))))

(defn stop-iteration-sd
  [iteration]
  (fn [snapshot]
    (let [{snapshot-iteration ::sim-engine/iteration} snapshot]
      ;;TODO Puis regarder pourquoi faire fonctionner les logs (or (nil? snapshot-iteration) (nil? iteration) (>= snapshot-iteration iteration))
      (when (or (nil? snapshot-iteration) (nil? iteration) (>= snapshot-iteration iteration))
        #::sim-engine{:doc (if (= iteration snapshot-iteration)
                             ["Stops at iteration %d as required" iteration]
                             ["Stops at iteration %d (requiring %d)" snapshot-iteration iteration])
                      :id ::iteration-stopping
                      :context {:stopping-iteration iteration
                                :snapshot-iteration snapshot-iteration}}))))

(defn stop-now-sd
  "Stops now."
  []
  (fn [_]
    #::sim-engine{:doc ["Stop now."]
                  :id ::stop-now
                  :context nil}))



(defn stop-state-contains-sd
  [state-path]
  (fn [snapshot]
    (let [snapshot-state (get snapshot ::sim-engine/state {})]
      (when-let [state-entry (get-in snapshot-state state-path)]
        #::sim-engine{:doc ["Stops as state contains `%s` in `%s`" state-entry state-path]
                      :id ::state-contains
                      :context #::sim-engine{:snapshot-state snapshot-state
                                             :state-entry state-entry}}))))

;; ********************************************************************************
;; Public API
;; ********************************************************************************

(defn stop-bucket
  "Stops when the the `stopping-bucket` is reached, or after."
  [model stopping-bucket]
  (-> model
      (add-to-stopping-definition (stop-bucket-sd stopping-bucket))))

(defn stop-iteration
  "Stops at iteration `iteration`"
  [model iteration]
  (-> model
      (add-to-stopping-definition (stop-iteration-sd iteration))))

(defn stop-now
  [model]
  (-> model
      (add-to-stopping-definition (stop-now-sd))))

(defn stop-state-contains
  [model state-path]
  (-> model
      (add-to-stopping-definition (stop-state-contains-sd state-path))))

(defn eval
  "Evaluates the `stopping-fns` on `snapshot`.

  Returns `nil` if `stopping-evaluation` is not defined.
  Returns a map with `stop?` and `context`."
  [stopping-fns snapshot]
  (reduce (fn [res stopping-fn]
            (if-let [v (stopping-fn snapshot)]
              (conj res v)
              res))
          []
          stopping-fns))

