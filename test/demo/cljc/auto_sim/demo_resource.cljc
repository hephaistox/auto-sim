(ns auto-sim.demo-resource
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-opti.distribution           :as opt-distribution]
   [auto-opti.prng                   :as opt-prng]
   [auto-sim.engine                  :as sim-engine]
   [auto-sim.entity                  :as sim-entity]
   [auto-sim.machine                 :as sim-machine]
   [auto-sim.ordering                :as sim-ordering]
   [auto-sim.printer.rc-entity-route :as sim-printer-rc-entity-route]
   [auto-sim.rc                      :as sim-rc]
   [auto-sim.rc.resource.consumption :as sim-rc-consumption]
   [auto-sim.rc.unqueueing-policy    :refer [fifo]]
   [auto-sim.route                   :as sim-route]
   [clojure.string                   :as str]))

(defn clean-ns-kw
  [m]
  (-> m
      (update-keys (comp keyword name))))

(def model-data
  #::sim-engine{:starting-bucket 0
                :waiting-time 0
                :max-nb-entity 4
                :routes {:blue [{:m :transp
                                 :pt 5}
                                {:m :m4
                                 :pt 1}
                                {:m :transp
                                 :pt 4}
                                {:m :m2
                                 :pt 3}
                                {:m :transp
                                 :pt 7}
                                {:m :m1
                                 :pt 2}]
                         :purple [{:m :transp
                                   :pt 5}
                                  {:m :m4
                                   :pt 1}
                                  {:m :transp
                                   :pt 4}
                                  {:m :m3
                                   :pt 3}
                                  {:m :transp
                                   :pt 4}
                                  {:m :m1
                                   :pt 1}]}
                ::seed #uuid "e85427c1-ed25-4ed4-9b11-52238d268265"})

(defn event-types
  [model-data prng]
  (let [#::sim-engine{:keys [routes]} model-data
        colors (-> routes
                   keys
                   vec)
        prio (sim-rc-consumption/compare-by-order [:high])
        color-distribution (opt-distribution/distribution {:prng prng
                                                           :params {:a 0
                                                                    :b (count colors)}
                                                           :dst-name :uniform-int})]
    {:CE (fn [event-return event bucket]
           (let [product-data (->> (nth colors (opt-distribution/draw color-distribution))
                                   (sim-route/entity-data routes))]
             (-> event-return
                 (sim-entity/schedule-entity-every event bucket ::product product-data)
                 (sim-entity/schedule-new-entity event bucket #::sim-engine{:type :PS}))))
     :PS (fn [event-return event bucket]
           (if-let [route (sim-route/get-route event-return event bucket)]
             (-> event-return
                 (sim-route/next-op event bucket route)
                 (sim-route/schedule event bucket #::sim-engine{:type :IS}))
             (-> event-return
                 (sim-entity/schedule event bucket #::sim-engine{:type :PT}))))
     :IS
     (fn [event-return event bucket]
       (let [evt
             (sim-route/add-current-operation event-return event bucket #::sim-engine{:type :MP})]
         (-> event-return
             (sim-rc/seize event bucket ::machine 1 evt :high))))
     :MP (fn [event-return event bucket]
           (-> event-return
               (sim-machine/infinite-capacity event bucket #::sim-engine{:type :OS} :pt)))
     :OS (fn [event-return event bucket]
           (-> event-return
               (sim-rc/dispose event bucket ::machine 1 fifo prio)
               (sim-route/schedule event bucket #::sim-engine{:type :MT})))
     :MT (fn [event-return event bucket]
           (if-let [route (sim-route/get-route event-return event bucket)]
             (-> event-return
                 (sim-route/next-op event bucket route)
                 (sim-route/schedule event bucket #::sim-engine{:type :IS}))
             (-> event-return
                 (sim-entity/schedule event bucket #::sim-engine{:type :PT}))))
     :PT sim-entity/sink}))

(defn create-model
  [model-data]
  (let [{::sim-engine/keys [starting-bucket waiting-time max-nb-entity]} model-data
        evt1 (sim-entity/n-entities-event starting-bucket max-nb-entity waiting-time :CE)]
    (assoc (-> model-data
               (sim-engine/initial-snapshot starting-bucket {} [evt1])
               (sim-rc/define-resource nil nil ::machine {}))
           ::sim-engine/sorter (sim-ordering/sorter (sim-ordering/fields ::sim-engine/bucket)
                                                    (sim-ordering/types
                                                     [:CE :PS :IS :MP :OS :MT :PT])
                                                    (sim-ordering/fields ::product))
           ::sim-engine/event-registry (event-types model-data
                                                    (opt-prng/xoroshiro128 (::seed model-data))))))

(deftest assembly-test
  (is (= (let [{::sim-engine/keys [routes max-nb-entity]} model-data
               nb-operations (-> routes
                                 first
                                 second
                                 count)
               nb-events-for-operations (* 4 nb-operations) ;; Each operation has IS, MP, OS and MT
               w-starting-and-ending (+ 2 nb-events-for-operations 1) ;; Each entity starts with CE and a MT and ends with PT
              ]
           (* max-nb-entity w-starting-and-ending))
         (-> (-> model-data
                 create-model
                 sim-engine/run)
             ::sim-engine/past-events
             count))))

(defn past-events
  [model entity-translation]
  (->> model
       ::sim-engine/past-events
       (mapv (fn [{::sim-engine/keys [type bucket entity-uuid]
                   :as _event}]
               (cond
                 (sim-printer-rc-entity-route/check type [:CE])
                 (println (str bucket
                               "-new entity ("
                               (sim-printer-rc-entity-route/uuid-idx entity-translation entity-uuid)
                               ")")))))))

(defn entities
  [model entity-translation]
  (doseq [[entity-uuid entity] (-> model
                                   ::sim-engine/state
                                   ::sim-engine/entity)]
    (let [{::sim-engine/keys [entity-state]} entity
          {::sim-engine/keys [route-id route]} entity-state]
      (println
       (str "e(" (sim-printer-rc-entity-route/uuid-idx entity-translation entity-uuid)
            "), route " (name route-id)
            "=" (str/join "," (mapv (fn [{:keys [m pt]}] (str "(" (name m) "," pt ")")) route))))))
  model)

(defn next-event
  [model entity-translation]
  (->> model
       ::entity-future-events
       first
       (mapv (fn [evt] (:k)))))

(defn snapshot!
  [model]
  (let [{::sim-engine/keys [bucket id iteration]} model]
    (println (apply str (repeat 80 "*")))
    (println "Iteration " iteration ", bucket" bucket ", id" id))
  model)

(defn errors
  [model]
  (let [{::sim-engine/keys [stopping-criteria]} model]
    (when stopping-criteria
      (doseq [{::sim-engine/keys [doc]} stopping-criteria]
        (println "Errors:")
        (when doc (println (apply format doc))))))
  model)

(let [entity-translation (sim-printer-rc-entity-route/create-translation)]
  (reduce (fn [model i]
            (println "model" model)
            (if (::stopping-criteria model)
              model
              (-> model
                  (sim-engine/run-iteration i)
                  snapshot!
                  errors
                  ;(past-events new-model entity-translation)
                  ;(entities entity-translation)
                  #_sim-engine/clean-stop-criteria)))
          (create-model model-data)
          (range 2 10)))

(sim-engine/run-iteration (create-model model-data) 3)
