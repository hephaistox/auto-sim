(ns auto-sim.component.time-picker
  "A time picker allows the user to manually select the time in a predefined range.

  The range could be:
  - completly predefined with `min` and `max`.
  - with a `max` to be discoverd when you advance in time. In that case `last`"
  (:require
   [auto-opti.maths :refer [ceil]]
   [auto-sim        :as-alias sim]))

(def after-last "When the last element is not reached, this extends the range" 1.2)

(defn- clamp-time
  "Time picker is defaulted to minimum, clamped between min and max."
  [t min max]
  (cond-> (or t min)
    max (clojure.core/min max)
    min (clojure.core/max min)))

(defn get-time
  "Returns `t` time which is defaulted and clamped."
  [{:keys [min max]
    :as tp-data}
   tp-vals]
  (-> (or (:time tp-vals) (:time tp-data) min 0)
      (clamp-time min max)))

(defn update-time
  "Update `tp-vals` so time is set to new-time:
  - The value is clamped if needed."
  [tp-vals new-time]
  (assoc tp-vals :time new-time))

(defn time-picker
  "A time picker is selecting one of date in a process between `min` and `max`.

  Two modes exist:
  - If `max` is non `nil`, the range is between `min` and `max`
  - Otherwise, the range is between `min` and `max` augmented with `after-last`"
  [opts tp-data tp-vals change-value-fn dynamic-max-fn]
  (let [[opts* tp-data* tp-vals* change-value-fn* dynamic-max-fn*]
        (if (fn? tp-vals)
          [{} opts tp-data tp-vals change-value-fn dynamic-max-fn]
          [opts tp-data tp-vals change-value-fn dynamic-max-fn])
        {:keys [fast-step step min]
         :or {fast-step 10
              step 1
              min 0}}
        tp-data*
        max (:max tp-data*)
        time (or (:time tp-vals*) (:time tp-data*) min 0)
        max (if max
              max
              (when (fn? dynamic-max-fn*)
                (when-let [new-max (dynamic-max-fn* tp-vals*)]
                  (change-value-fn* new-max)
                  new-max)))
        time (clamp-time time min max)
        max-not-defined (and (nil? max) (not (fn? dynamic-max-fn)))]
    [:div.w3-bar
     opts*
     [:div.fa.fa-fast-backward.w3-bar-item {:class (if max-not-defined "w3-text-grey" "w3-button")
                                            :on-click #(when (and (not max-not-defined)
                                                                  (fn? change-value-fn*))
                                                         (-> (- time fast-step)
                                                             (clamp-time min max)
                                                             change-value-fn*))}]
     [:div.fa.fa-step-backward.w3-bar-item {:class (if max-not-defined "w3-text-grey" "w3-button")
                                            :on-click #(when (and (not max-not-defined)
                                                                  (fn? change-value-fn*))
                                                         (-> (- time step)
                                                             (clamp-time min max)
                                                             change-value-fn*))}]
     [:div.w3-bar-item
      [:input {:type "range"
               :min min
               :disabled max-not-defined
               :value time
               :max (if max max (ceil (* after-last time)))
               :style (when-not max-not-defined {:cursor "grab"})
               :on-change #(when (and (not max-not-defined) (fn? change-value-fn*))
                             (-> %
                                 .-target
                                 .-value
                                 int
                                 (clamp-time min max)
                                 change-value-fn*))}]]
     [:div.fa.fa-step-forward.w3-bar-item {:class (if max-not-defined "w3-text-grey" "w3-button")
                                           :on-click #(when (and (not max-not-defined)
                                                                 (fn? change-value-fn*))
                                                        (-> (+ time step)
                                                            (clamp-time min max)
                                                            change-value-fn*))}]
     [:div.fa.fa-fast-forward.w3-bar-item {:class (if max-not-defined "w3-text-grey" "w3-button")
                                           :on-click #(when (and (not max-not-defined)
                                                                 (fn? change-value-fn*))
                                                        (-> (+ time fast-step)
                                                            (clamp-time min max)
                                                            change-value-fn*))}]]))
