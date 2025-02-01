#_{:heph-ignore {:forbidden-words ["tap>"]}}
(ns auto-sim.demo.control
  "Demo usefull for control toy example"
  (:require
   [auto-sim.control                        :as sim-de-control]
   [auto-sim.demo.data                      :as sim-demo-data]
   [auto-sim.demo.simulation-engine         :as sim-demo-engine]
   [auto-sim.simulation-engine              :as sim-engine]
   [auto-sim.simulation-engine.event-return :as sim-de-event-return]))

(defn infinite-part-terminate
  [{::sim-engine/keys [date]
    ::sim-demo-data/keys [product _machine]}
   state
   future-events]
  (-> #::sim-engine{:state state
                    :future-events future-events}
      (sim-de-event-return/add-event {::sim-engine/type :MA
                                      ::sim-engine/date date
                                      ::sim-demo-data/product product
                                      ::sim-demo-data/machine :m1})))

(defn registries
  ([] (registries false))
  ([infinite?]
   (let [events (cond-> sim-demo-engine/events
                  infinite? (assoc :PT infinite-part-terminate))]
     (-> (sim-engine/registries)
         (update ::sim-engine/event merge events)
         sim-de-control/wrap-registry))))

(defn model [] (sim-demo-engine/model sim-demo-data/model-data (registries)))

(defn model-infinite [] (sim-engine/build-model sim-demo-data/model-data (registries true)))

(defn model-early-end
  []
  (sim-engine/build-model (update sim-demo-data/model-data
                                  ::sim-engine/stopping-criterias
                                  conj
                                  [::sim-engine/iteration-nth {::sim-engine/n 20}])
                          (registries)))


(defn control-state
  []
  (sim-de-control/build-rendering-state {:computation (sim-de-control/make-computation (model)
                                                                                       :direct)}))
