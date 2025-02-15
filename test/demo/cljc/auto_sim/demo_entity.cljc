(ns auto-sim.demo-entity
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-opti.distribution :as opt-distribution]
   [auto-opti.prng         :as opt-prng]
   [auto-sim.demo-data     :as sim-demo-data]
   [auto-sim.engine        :as sim-engine]
   [auto-sim.entity        :as sim-entity]
   [auto-sim.ordering      :as sim-ordering]))

(def model-data
  #::sim-engine{:future-events [{::sim-engine/type :CE
                                 ::sim-engine/bucket 0
                                 ::waiting-time 2
                                 ::max-nb-entity 4}]
                :routes {:blue [{:m :m4
                                 :tt 2
                                 :pt (:m4 sim-demo-data/process-time)}
                                {:m :m2
                                 :tt 2
                                 :pt (:m2 sim-demo-data/process-time)}
                                {:m :m1
                                 :tt 0
                                 :last? true
                                 :pt (:m1 sim-demo-data/process-time)}]
                         :purple [{:m :m4
                                   :tt 2
                                   :pt (:m4 sim-demo-data/process-time)}
                                  {:m :m3
                                   :tt 2
                                   :pt (:m3 sim-demo-data/process-time)}
                                  {:m :m1
                                   :last? true
                                   :tt 0
                                   :pt (:m1 sim-demo-data/process-time)}]}
                ::seed #uuid "e85427c1-ed25-4ed4-9b11-52238d268265"})

(def colors [:blue :purple])

(defn color-distribution
  [prng]
  (opt-distribution/distribution {:prng prng
                                  :params {:a 0
                                           :b (count colors)}
                                  :dst-name :uniform-int}))

(defn event-types
  [model-data prng]
  (let [#::sim-engine{:keys [routes]} model-data]
    {:CE (fn [event bucket state future-events]
           (let [product-id (sim-entity/create-id)
                 {::keys [nb-entity max-nb-entity waiting-time]
                  :or {nb-entity 0}}
                 event
                 entity-color (->> (opt-distribution/draw (color-distribution prng))
                                   (nth colors))
                 route (get routes entity-color)
                 state (-> state
                           (sim-entity/create bucket
                                              :product-name
                                              product-id
                                              {::color entity-color
                                               ::route route}))]
             #::sim-engine{:state state
                           :future-events (cond-> (conj future-events
                                                        {::sim-engine/type :MA
                                                         ::sim-entity/entity-id product-id
                                                         ::sim-engine/bucket bucket})
                                            (< nb-entity max-nb-entity)
                                            (conj (assoc event
                                                         ::sim-engine/bucket (+ bucket waiting-time)
                                                         ::nb-entity (inc nb-entity))))}))
     :MA (fn [event bucket state future-events]
           (let [[first-operation rroute] (->> (sim-entity/state state event)
                                               ::route
                                               ((juxt peek pop)))
                 state (sim-entity/update state bucket event (comp pop ::sim-entity/route))]
             #::sim-engine{:state state
                           :future-events (conj future-events
                                                {::sim-engine/type :MP
                                                 ::first-operation first-operation
                                                 ::rroute rroute
                                                 ::sim-engine/bucket bucket})}))
     :MP (fn [event bucket state future-events]
           (let [{::keys [first-operation]} event
                 {:keys [pt]} first-operation]
             #::sim-engine{:state state
                           :future-events (conj future-events
                                                (assoc event
                                                       ::sim-engine/type :MT
                                                       ::sim-engine/bucket (+ bucket pt)))}))
     :MT (fn [{::keys [first-operation]} bucket state future-events]
           (let [{:keys [tt last?]} first-operation]
             #::sim-engine{:state state
                           :future-events (cond-> future-events
                                            (not last?) (conj {::sim-engine/type :MA
                                                               ::sim-engine/bucket (+ bucket
                                                                                      tt)}))}))
     :PT (fn [event bucket state future-events]
           #::sim-engine{:state (sim-entity/dispose state bucket event)
                         :future-events future-events})}))

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
