(ns auto-sim.printer.rc-entity-route
  (:require
   [auto-sim.engine :as sim-engine]
   [auto-sim.entity :as sim-entity]))

(defn create-translation "Returns a translation" [] (atom {}))

(defn uuid-idx
  [translation uuid]
  (if-let [idx (get @translation uuid)]
    idx
    (let [idx (count @translation)]
      (swap! translation assoc uuid idx)
      idx)))

(defn- check
  [value k]
  (if (vector? k)
    (->> k
         (filter #(= % value))
         seq)
    (= k value)))

(defn returns
  "a"
  [model product-creation product-termination route-next]
  (let [entity-translation (create-translation)]
    (->>
      (-> (sim-engine/run model)
          ::sim-engine/past-events)
      (mapv
       (fn [{::sim-engine/keys [type bucket entity-id current-operation route-id]
             ::sim-entity/keys [waiting-time nb-entity max-nb-entity]}]
         (cond
           (check type product-creation) (format "%03d %s, create product %02d/%02d, next in %d"
                                                 bucket
                                                 (name type)
                                                 nb-entity
                                                 max-nb-entity
                                                 waiting-time)
           (check type route-next) (format "%03d %s (e%s, %6s)"
                                           bucket
                                           (name type)
                                           (uuid-idx entity-translation entity-id)
                                           (or (some-> (:m current-operation)
                                                       name)
                                               ""))
           (check type product-termination) (format "%03d %s stops product e%s"
                                                    bucket
                                                    (name type)
                                                    (uuid-idx entity-translation entity-id))
           :else (format "%03d %s (e%s, %6s, route=%8s) pt=%02d"
                         bucket
                         (name type)
                         (uuid-idx entity-translation entity-id)
                         (or (some-> (:m current-operation)
                                     name)
                             "none")
                         (or (some-> route-id
                                     name)
                             "N/A")
                         (:pt current-operation))))))))
