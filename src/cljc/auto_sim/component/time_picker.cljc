(ns auto-sim.component.time-picker
  "A time picker selects time in a predefined range.

  The maximum is a special case that can be discovered.
  While it is not reached, the component is in a special mode remembering the last element met but not displaying the maximum but a space after the cursor to tell some further dates are possible."
  (:require
   [auto-opti.maths            :refer [ceil]]
   [auto-sim                   :as-alias sim]
   [auto-web.components.button :refer [clink-button]]
   [cljs.math]))

(def after-last "When the last element is not reached, this extends the range" 1.2)

(defn clamp-time-picker
  "Time picker is defaulted to minimum, clamped between min and max."
  [t min max]
  (cond-> (or t min)
    max (clojure.core/min max)
    min (clojure.core/max min)))

(defn update-time
  "Update to new-time-fn, clamp the value if needed"
  [{:keys [min max last time]
    :as v}
   new-time-fn]
  (let [new-time (clamp-time-picker (new-time-fn time) min max)
        v (assoc v :time new-time)]
    (cond-> v
      (nil? max) (assoc :last (clojure.core/max last 1 (ceil (* after-last new-time-fn)))))))

(defn time-picker
  [opts tp time change-max-fn change-value-fn]
  (let [opts* (if (map? opts) opts {})
        tp* (if (map? opts) tp opts)
        time* (if (map? opts) time tp)
        change-max-fn* (if (map? opts) change-max-fn time)
        change-value-fn* (if (map? opts) change-value-fn change-max-fn)
        {:keys [fast-step step min last max]
         :or {min 0
              step 1
              fast-step 10}}
        tp*]
    (when-let [new-max (when (fn? change-max-fn*) (change-max-fn* max))] (change-max-fn* new-max))
    [:div.w3-bar
     opts*
     [clink-button {:class "fa fa-fast-backward w3-bar-item"
                    :on-click #(change-value-fn* (- % fast-step))}]
     [clink-button {:class "fa fa-step-backward w3-bar-item"
                    :on-click #(change-value-fn* (- % step))}]
     [:div.w3-bar-item
      [:input {:type "range"
               :min min
               :value (clink-button time* min max)
               :max (or max last)
               :style {:cursor "grab"}
               :on-change #(change-value-fn* (-> %
                                                 .-target
                                                 .-value
                                                 int))}]]
     [clink-button {:class "fa fa-step-forward w3-bar-item"
                    :on-click #(change-value-fn* (+ % step))}]
     [clink-button {:class "fa fa-fast-forward w3-bar-item"
                    :on-click #(change-value-fn* (+ % fast-step))}]]))
