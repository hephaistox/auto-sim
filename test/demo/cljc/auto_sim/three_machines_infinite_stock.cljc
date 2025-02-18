(ns auto-sim.three-machines-infinite-stock
  "Example of thee routings, with no intermediate stock."
  (:require
   [auto-sim.engine   :as sim-engine]
   [auto-sim.ordering :as sim-ordering]))

(def order-stub
  (sim-ordering/sorter (sim-ordering/fields ::sim-engine/bucket)
                       (sim-ordering/types [:new-op :new-job])))

(def routings
  [[{:m :m2
     :p 3}
    {:m :m1
     :p 4}
    {:m :m3
     :p 2}]
   [{:m :m3
     :p 2}
    {:m :m2
     :p 4}
    {:m :m1
     :p 3}]])

(def max-jobs 2)

(defn new-job
  [event-return _ bucket]
  (let [{::sim-engine/keys [state future-events]} event-return
        {:keys [n-jobs]} state
        routing (rand-nth routings)]
    #::sim-engine{:state (-> state
                             (assoc :n-jobs (inc n-jobs)))
                  :future-events (cond-> future-events
                                   (< n-jobs (dec max-jobs)) (conj #::sim-engine{:type :new-job
                                                                                 :bucket
                                                                                 (+ 100 bucket)})
                                   :else (conj #::sim-engine{:type :new-op
                                                             :routings routing
                                                             :bucket bucket}))}))

(defn new-op
  [event-return event bucket]
  (let [{::sim-engine/keys [routings]} event
        [routing & rroutings] routings]
    (-> event-return
        (update ::sim-engine/future-events
                #(cond-> %
                   routing (conj #::sim-engine{:type :new-op
                                               :routings (vec rroutings)
                                               :bucket (+ bucket (:p routing))}))))))

(->> (sim-engine/initial-snapshot 0
                                  {:n-jobs 0}
                                  [#::sim-engine{:type :new-job
                                                 :bucket 0}])
     (sim-engine/continue #::sim-engine{:sorter order-stub
                                        :event-registry {:new-job new-job
                                                         :new-op new-op}}))
