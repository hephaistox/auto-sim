(ns auto-sim.demo-entity
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-opti.distribution :as opt-distribution]
   [auto-opti.prng         :as opt-prng]
   [auto-sim.demo-data     :as sim-demo-data]
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
  #::sim-engine{:future-events [(sim-entity/n-entities-event 0 1 100 :CE)]
                :routes {:blue [{:m :m4
                                 :idx 0
                                 :tt 2
                                 :pt 1}
                                {:m :m2
                                 :idx 1
                                 :tt 2
                                 :pt 3}
                                {:m :m1
                                 :idx 2
                                 :pt 1}]
                         :purple [{:m :m4
                                   :idx 0
                                   :tt 2
                                   :pt 1}
                                  {:m :m3
                                   :idx 1
                                   :tt 2
                                   :pt 3}
                                  {:m :m1
                                   :tt 1
                                   :idx 2
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
           (let [product-data (->> (opt-distribution/draw color-distribution)
                                   (nth colors)
                                   (sim-route/entity-data routes))]
             (-> event-return
                 (sim-entity/schedule-entity-every event bucket :product product-data)
                 (sim-entity/schedule #::sim-engine{:type :MA} bucket nil))))
     :MA (fn [event-return event bucket]
           (sim-route/pop event-return event bucket #::sim-engine{:type :MP}))
     :MP (fn [event-return event bucket]
           (sim-machine/infinite-capacity event-return event bucket #::sim-engine{:type :MT} :pt))
     :MT
     (fn [event-return event bucket]
       (let [{::sim-engine/keys [route]} (sim-entity/state (::sim-engine/state event-return) event)]
         (if (empty? route)
           (sim-entity/schedule-same-entity event-return event bucket #::sim-engine{:type :PT})
           ;;TODO J'en suis là. Faut que je mette
           (sim-machine/infinite-capacity event-return event bucket #::sim-engine{:type :MA} :tt))))
     :PT sim-entity/sink}))

(def order
  (sim-ordering/sorter (sim-ordering/fields ::sim-engine/bucket)
                       (sim-ordering/types [:CE :MA :MP :MT :PT])
                       (sim-ordering/fields ::sim-demo-data/product)))

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
