(ns auto-sim.animation
  "Animation is moving render-element across time

  The uuid is ensuring only one thread of ::sim/iterate is happening. If a new start is made, the uuid will be different and the older iterate will automatically stop"
  (:require
   [auto-sim      :as-alias sim]
   [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]))

(defn trajectory
  [t src dst]
  (let [{x-src :x
         y-src :y
         t-src :t}
        src
        {x-dst :x
         y-dst :y
         t-dst :t}
        dst
        i (/ (- t t-src) (- t-dst t-src))]
    (cond
      (< t t-src) src
      (> t t-dst) dst
      :else {:x (+ x-src (* i (- x-dst x-src)))
             :y (+ y-src (* i (- y-dst y-src)))})))

(reg-event-db ::sim/start
              (fn [db [_ k refresh-interval]]
                (let [uuid (random-uuid)]
                  (dispatch [::sim/iterate k uuid refresh-interval])
                  (update-in db
                             [::sim/animation k]
                             assoc
                             :start-time (.now js/Date)
                             :uuid uuid
                             :refresh-inteval refresh-interval))))

(reg-event-db ::sim/stop (fn [db [_ k]] (update-in db [::sim/animation k] dissoc :uuid)))

(reg-event-fx ::sim/iterate
              (fn [{:keys [db]} [_ k uuid refresh-interval]]
                (let [v (get-in db [::sim/animation k])]
                  (when (= uuid (:uuid v))
                    (let [{:keys [speed start-time]
                           :or {speed 1}}
                          v
                          ms (* speed (- (.now js/Date) start-time))]
                      {:db (assoc-in db [::sim/time-picker :animation :time] (/ ms 100))
                       :fx [[:dispatch-later {:ms refresh-interval
                                              :dispatch
                                              [::sim/iterate k uuid refresh-interval]}]]})))))
