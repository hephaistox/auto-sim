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
   [auto-sim.printer.rc-entity-route :as    sim-printer-rc-entity-route
                                     :refer [uuid-idx]]
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
                ::resource-input {:m1 {}
                                  :m2 {}
                                  :m3 {}
                                  :m4 {}}
                :waiting-time 0
                :max-nb-entity 2
                :routes {:blue [{:m :m4
                                 :pt 1}
                                {:m :m2
                                 :pt 3}
                                {:m :m1
                                 :pt 2}]
                         :purple [{:m :m4
                                   :pt 1}
                                  {:m :m3
                                   :pt 3}
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
       (let [{:keys [m _pt]} (::sim-engine/current-operation event)
             evt
             (sim-route/add-current-operation event-return event bucket #::sim-engine{:type :MP})]
         (-> event-return
             (sim-rc/seize event bucket m 1 evt :high))))
     :MP (fn [event-return event bucket]
           (-> event-return
               (sim-machine/infinite-capacity event bucket #::sim-engine{:type :OS} :pt)))
     :OS (fn [event-return event bucket]
           (let [{:keys [m _pt]} (::sim-engine/current-operation event)]
             (-> event-return
                 (sim-rc/dispose event bucket m 1 fifo prio)
                 (sim-route/schedule event bucket #::sim-engine{:type :MT}))))
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
               (sim-rc/define-resource nil nil :m1 {})
               (sim-rc/define-resource nil nil :m2 {})
               (sim-rc/define-resource nil nil :m3 {})
               (sim-rc/define-resource nil nil :m4 {}))
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
       (mapv (fn [{::sim-engine/keys [type bucket entity-id]
                   :as _event}]
               (cond
                 (sim-printer-rc-entity-route/check type [:CE])
                 (println
                  (str bucket "-new entity (" (uuid-idx entity-translation entity-id) ")")))))))

(defn entities
  [model entity-translation]
  (doseq [[entity-id entity] (-> model
                                 ::sim-engine/state
                                 ::sim-engine/entity)]
    (let [{::sim-engine/keys [entity-state]} entity
          {::sim-engine/keys [route-id route]} entity-state]
      (println (str "- e(" (uuid-idx entity-translation entity-id)
                    "), route " (name route-id)
                    " = " (str/join ", "
                                    (mapv (fn [{:keys [m pt]}] (str "(" (name m) "," pt ")"))
                                          route))))))
  model)

(defn separator [model] (println (apply str (repeat 80 "*"))) model)

(defn snapshot
  [model]
  (let [{::sim-engine/keys [bucket _id iteration]} model]
    (println (str "Iteration " iteration ", bucket " bucket)))
  model)

(defn snapshot-header
  [model]
  (let [{::sim-engine/keys [bucket iteration]} model] (print (str iteration " t(" bucket ") ")))
  model)

(defn event*
  [event entity-translation]
  (let [{::sim-engine/keys [type bucket entity-id _route-id current-operation]
         ::sim-entity/keys [waiting-time nb-entity max-nb-entity]}
        event
        {:keys [m pt]} current-operation]
    (->> (concat [(name type)]
                 (case type
                   :CE (concat [", creates entity " (inc nb-entity) " of " max-nb-entity]
                               (if (>= (inc nb-entity) max-nb-entity)
                                 [", last one"]
                                 [", next in " waiting-time]))
                   :PS [", starts product e(" (uuid-idx entity-translation entity-id) ")"]
                   :IS [", e(" (uuid-idx entity-translation entity-id) ") seizes in " m]
                   :MP [", e("
                        (uuid-idx entity-translation entity-id)
                        ") starts production on "
                        m
                        " during "
                        pt
                        " buckets"]
                   :OS [", e(" (uuid-idx entity-translation entity-id) ") ends production on " m]
                   :MT [", e(" (uuid-idx entity-translation entity-id) ") quits machine " m]
                   :PT [", ends product e(" (uuid-idx entity-translation entity-id) ")"]
                   [])
                 [", ends in " bucket " bucket"])
         (apply str)
         println)))

(defn resources
  [model entity-translation]
  (doseq [[resource-id resource] (get-in model [::sim-engine/state ::sim-engine/resource])]
    (let [{::sim-engine/keys [capacity consumption queue]} resource
          prefix "    "]
      (println (str "* " (name resource-id) " capa=" capacity))
      (doseq [[_ consumption-detail] consumption]
        (let [{::sim-engine/keys [consumption-quantity _priority entity-id]} consumption-detail]
          (println
           (str prefix "e(" (uuid-idx entity-translation entity-id) ") " consumption-quantity))))
      (doseq [queue-item queue]
        (let [{::sim-engine/keys [event _priority consumption-quantity]} queue-item]
          (print (str prefix "\\-> quantity " consumption-quantity " required by "))
          (event* event entity-translation)))))
  model)

(defn next-event
  [model entity-translation]
  (-> model
      ::sim-engine/future-events
      first
      (event* entity-translation))
  model)

#?(:cljc (do (defn errors
               [model]
               (let [{::sim-engine/keys [stopping-criteria]} model]
                 (if (seq stopping-criteria)
                   (doseq [{::sim-engine/keys [doc]} stopping-criteria]
                     (when doc (println (apply format doc))))
                   model)))
             (let [entity-translation (sim-printer-rc-entity-route/create-translation)]
               (reduce (fn [model i]
                         (if (seq (::sim-engine/stopping-criteria model))
                           model
                           (some-> model
                                   (sim-engine/run-iteration i)
                                   separator
                                   snapshot
                                   errors
                                   ;(entities entity-translation)
                                   (resources entity-translation)
                                   (next-event entity-translation))))
                       (create-model model-data)
                       (range 1 100)))
             (-> model-data
                 create-model
                 (sim-engine/run-iteration 9))))

(comment
 ;
)
