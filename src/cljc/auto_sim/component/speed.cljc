(ns auto-sim.component.speed
  "Speed controls the animation time"
  (:require
   [auto-sim              :as-alias sim]
   [auto-web.page.builder :refer [merge-opts]]))

(defn- index-of
  [v target*]
  (some (fn [[idx val]] (when (= val target*) idx)) (map-indexed vector v)))

(defn- clamp-idx
  [idx n]
  (some-> idx
          (min (dec n))
          (max 0)))

(defn time-speed
  [opts vals val on-change-fn default-value]
  (let [opts* (if (map? opts) opts {})
        vals* (if (map? opts) vals opts)
        val* (if (map? opts) val vals)
        on-change-fn* (if (map? opts) on-change-fn val)
        default-value* (if (map? opts) default-value on-change-fn)]
    [:input
     (merge-opts {:style {:height "2em"
                          :cursor "grab"}
                  :min 0
                  :max (dec (count vals*))
                  :value (or (index-of vals* val*)
                             (index-of vals* default-value*)
                             (index-of vals* (first vals*)))
                  :class "w3-light-grey w3-round-xlarge"
                  :type "range"
                  :on-change (fn [e]
                               (when (fn? on-change-fn*)
                                 (let [target* (clamp-idx (-> e
                                                              .-target
                                                              .-value
                                                              int)
                                                          (count vals*))]
                                   (on-change-fn* (get vals* target*)))))}
                 opts*)]))
