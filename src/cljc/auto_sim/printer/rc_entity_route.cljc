(ns auto-sim.printer.rc-entity-route
  "Printer for simulation with resources, entities and routes

  All values returned are a collection of printer item - a list of:
  * string
  * values to be formatted with the string"
  (:require
   [auto-sim.engine :as sim-engine]
   [auto-sim.entity :as sim-entity]
   [clojure.string  :as str]))

;; ********************************************************************************
;; Internal API
;; ********************************************************************************

(defn create-translation "Returns a translation" [] (atom {}))

(defn uuid-idx
  [translation uuid]
  (if-let [idx (get @translation uuid)]
    idx
    (let [idx (count @translation)]
      (swap! translation assoc uuid idx)
      idx)))

(defn check
  [value k]
  (if (vector? k)
    (->> k
         (filter #(= % value))
         seq)
    (= k value)))

;; ********************************************************************************
;; Below is garbage
;; ********************************************************************************

(defn stopping-criteria*
  [stopping-criteria]
  (let [#::sim-engine{:keys [doc]} stopping-criteria]
    {:id :stopping-criteria
     :doc doc}))

(defn snapshot-iteration*
  [model]
  (let [{::sim-engine/keys [iteration]} model] {:iteration iteration}))

(defn consumption*
  [resource-id consumption-uuid consumption consumption-translation entity-translation]
  {:id :consumption
   :resource-id resource-id
   :quantity (::sim-engine/consumption-quantity consumption)
   :prio (::sim-engine/priority consumption)
   :entity-id (uuid-idx entity-translation (::sim-engine/entity-id consumption))
   :consumption-id (uuid-idx consumption-translation consumption-uuid)})

(defn event*
  [{::sim-engine/keys [type bucket entity-id current-operation route-id]
    ::sim-entity/keys [waiting-time nb-entity max-nb-entity]}
   {:keys [entity-creation product-start product-termination route-next skipped]}
   entity-translation]
  (when bucket
    (assoc (cond
             (check type entity-creation) (when nb-entity
                                            {:entity-nb (inc nb-entity)
                                             :max-nb-entity max-nb-entity
                                             :waiting-time waiting-time})
             (check type product-start) {:entity-uuid (uuid-idx entity-translation entity-id)}
             (check type route-next) {:entity-uuid (uuid-idx entity-translation entity-id)}
             (check type product-termination) {:entity-uuid (uuid-idx entity-translation entity-id)}
             (not (check type skipped)) {:entity-uuid (uuid-idx entity-translation entity-id)}
             :else nil)
           :id :entity
           :type type
           :bucket bucket)))

(defn entity*
  [[entity-uuid entity] entity-translation]
  (let [{::sim-engine/keys [entity-state]} entity
        {::sim-engine/keys [route]} entity-state]
    (->> route
         (map (fn [{:keys [m pt]}]
                (str "("
                     (some-> m
                             name)
                     ", "
                     pt
                     ")")))
         (str/join ", ")
         (cons (str "e" (uuid-idx entity-translation entity-uuid) "- "))
         (apply str))))

(defn queue*
  [queue par entity-translation]
  (->> queue
       (mapv (fn [{::sim-engine/keys [consumption-quantity]
                   evt-in-queue ::sim-engine/event}]
               (let [[estr & eargs] (event* evt-in-queue par entity-translation)]
                 (vec (concat [(str "%s items seized by" estr) consumption-quantity] eargs)))))))

(defn resource*
  [[resource-id resource] par consumption-translation entity-translation]
  (let [{::sim-engine/keys [capacity renewable?]
         resource-consumption ::sim-engine/consumption
         resource-queue ::sim-engine/queue}
        resource]
    (cond
      (seq resource-queue)
      ["rsc `%s`(%s) (%2d) [%s] waits for [%s]"
       (name resource-id)
       (if renewable? "r" "n")
       capacity
       (consumption* resource-id resource-consumption consumption-translation entity-translation)
       (queue* resource-queue par entity-translation)]
      (seq resource-consumption)
      (vec
       (concat
        [["rsc `%s`: busy, none is waiting" (name resource-id)]]
        (consumption* resource-id resource-consumption consumption-translation entity-translation)))
      :else ["rsc `%s`: idle" (name resource-id)])))

;; ********************************************************************************
;; Public API
;; ********************************************************************************

(defn consumption
  [resource-id consumption]
  (consumption* resource-id consumption (create-translation) (create-translation)))


(defn event
  "Display the event based on its `type`

  `par` describes list of `::sim-engine/type`
  * `entity`"
  [model par]
  (event* model par (create-translation)))

(defn past-events*
  [model par entity-translation]
  (vec (concat (when-let [stopping-criteria (::sim-engine/stopping-criteria model)]
                 (cons "Simulation has stopped with" stopping-criteria))
               (->> (::sim-engine/past-events model)
                    (map #(event* % par entity-translation))
                    (filterv some?)))))

(defn past-events
  "Returns past-events as strings of this model executed as strings"
  [model par]
  (past-events* model par (create-translation)))



(defn entity
  "Display entities in the `model`

  For a collection of
  *
  "
  [model translation]
  (let [{::sim-engine/keys [state]} model
        {::sim-engine/keys [entity]} state]
    (->> entity
         (mapv (fn [[entity-uuid entity]]
                 (let [{::sim-engine/keys [entity-state]} entity
                       {::sim-engine/keys [route]} entity-state]
                   (->> route
                        (map (fn [{:keys [m pt]}] (str "(" (name m) ", " pt ")")))
                        (str/join ", ")
                        (cons (str "e" (uuid-idx translation entity-uuid) "- "))
                        (apply str)))))))
  (entity* model (create-translation)))

(defn queue [queue par] (queue* queue par (create-translation)))

(defn resource [model par] (resource* model par (create-translation) (create-translation)))
(defn resource***
  [model par consumption-translation entity-translation]
  (let [{::sim-engine/keys [state]} model
        {::sim-engine/keys [resource]} state]
    (->> resource
         (mapv (fn [[resource-id resource]]
                 (let [{::sim-engine/keys [capacity renewable?]
                        resource-consumption ::sim-engine/consumption
                        resource-queue ::sim-engine/queue}
                       resource]
                   ["%s (%2d, %s) [%s] wait for [%s]"
                    (name resource-id)
                    capacity
                    (if renewable? "r" "n")
                    (str (consumption* resource-id
                                       resource-consumption
                                       consumption-translation
                                       entity-translation))
                    (str (queue* resource-queue par entity-translation))]))))))
