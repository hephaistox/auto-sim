(ns auto-sim.demo-entity
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-opti.distribution :as opt-distribution]
   [auto-opti.prng         :as opt-prng]
   [auto-sim.engine        :as sim-engine]
   [auto-sim.entity        :as sim-entity]
   [auto-sim.machine       :as sim-machine]
   [auto-sim.ordering      :as sim-ordering]
   [auto-sim.route         :as sim-route]))

(defn clean-ns-kw
  [m]
  (-> m
      (update-keys (comp keyword name))))

(def model-data
  #::sim-engine{:future-events [(sim-entity/n-entities-event 0 3 100 :CE)]
                :routes {:blue [{:m :transport
                                 :pt 5}
                                {:m :m4
                                 :pt 1}
                                {:m :transport
                                 :pt 4}
                                {:m :m2
                                 :pt 3}
                                {:m :transport
                                 :pt 7}
                                {:m :m1
                                 :pt 2}]
                         :purple [{:m :transport
                                   :pt 5}
                                  {:m :m4
                                   :pt 1}
                                  {:m :transport
                                   :pt 4}
                                  {:m :m3
                                   :pt 3}
                                  {:m :transport
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
        color-distribution (opt-distribution/distribution {:prng prng
                                                           :params {:a 0
                                                                    :b (count colors)}
                                                           :dst-name :uniform-int})]
    {:CE (fn [event-return event bucket]
           (let [product-data (->> (nth colors (opt-distribution/draw color-distribution))
                                   (sim-route/entity-data routes))]
             (-> event-return
                 (sim-entity/schedule-entity-every event bucket ::product product-data)
                 (sim-entity/schedule-new-entity event bucket #::sim-engine{:type :MT}))))
     :MP (fn [event-return event bucket]
           (sim-machine/infinite-capacity event-return event bucket #::sim-engine{:type :MT} :pt))
     :MT (fn [event-return event bucket]
           (if-let [route (sim-route/get-route event-return event bucket)]
             (-> event-return
                 (sim-route/next-op event bucket route)
                 (sim-route/schedule event bucket #::sim-engine{:type :MP}))
             (-> event-return
                 (sim-entity/schedule event bucket #::sim-engine{:type :PT}))))
     :PT sim-entity/sink}))

(def order
  (sim-ordering/sorter (sim-ordering/fields ::sim-engine/bucket)
                       (sim-ordering/types [:CE :PA :MA :MP :MT :PT])
                       (sim-ordering/fields ::product)))

(deftest assembly-test
  (is (= [9 21]
         (->> (sim-engine/initial-snapshot 0 {} (::sim-engine/future-events model-data))
              (sim-engine/continue
               #::sim-engine{:sorter order
                             :event-registry
                             (event-types model-data (opt-prng/xoroshiro128 (::seed model-data)))})
              ((juxt ::sim-engine/bucket ::sim-engine/iteration))))))

(-> (->> (sim-engine/initial-snapshot 0 {} (::sim-engine/future-events model-data))
         (sim-engine/continue #::sim-engine{:sorter order
                                            :event-registry (event-types model-data
                                                                         (opt-prng/xoroshiro128
                                                                          (::seed model-data)))}))
    (update ::sim-engine/past-events (partial mapv clean-ns-kw))
    (update ::sim-engine/state clean-ns-kw))
