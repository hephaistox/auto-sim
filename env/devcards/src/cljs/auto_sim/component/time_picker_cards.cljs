(ns auto-sim.component.time-picker-cards
  (:require
   [auto-sim.component.time-picker :refer [time-picker update-time]]
   [devcards.core                  :refer [defcard-rg]]
   [devcards.util.edn-renderer     :refer [html-edn]]
   [re-frame.core                  :refer [dispatch reg-event-db reg-sub subscribe]]
   [re-frame.db                    :refer [app-db]]
   [reagent.core                   :as reagent]))

(defn- focus-on
  [m path]
  (let [[a b] path]
    (-> m
        (select-keys [a])
        (update a select-keys [b]))))

(reg-event-db ::set-time-picker
              (fn [db [_ k time]] (update-in db [::time-picker k] update-time time)))

(reg-event-db ::set-max
              (fn [db [_ k max]] (update-in db [::time-picker k] assoc :max max :last max)))

(reg-sub ::time-picker (fn [db [_ k]] (get-in db [::time-picker k :time] 0)))

(defcard-rg
 time-picker-card
 "### A time picker
Starts with default at 35, step is 2, fast-step is 11. Min and max are known in advance: 30, 90"
 (fn [_ _]
   (reagent/as-element [:div
                        [time-picker {}
                         {:min 30
                          :max 90
                          :default 35
                          :step 2
                          :fast-step 11}
                         @(subscribe [::time-picker :std])
                         #(dispatch [::set-max :std %])
                         #(dispatch [::set-time-picker :std %])]
                        [:input {:value @(subscribe [::time-picker :std])
                                 :readOnly true}]
                        (html-edn (focus-on @app-db [::time-picker :std]))])))

(defcard-rg time-picker-init-below-min
            "When default value is 25, it is below the minimum, it is defaulted to the minimum 30"
            (fn [_ _]
              (reagent/as-element [:div
                                   [time-picker {}
                                    {:min 30
                                     :max 90
                                     :default 25
                                     :step 2
                                     :fast-step 11}
                                    @(subscribe [::time-picker :below-min])
                                    #(dispatch [::set-max :below-min %])
                                    #(dispatch [::set-time-picker :below-min %])]
                                   [:input {:value @(subscribe [::time-picker :below-min])
                                            :readOnly true}]
                                   (html-edn (focus-on @app-db [::time-picker :below-min]))])))

(defcard-rg time-picker-not-initialized
            "Time picker not initialized is suppose to start at 0"
            (fn [_ _]
              (reagent/as-element [:div
                                   [time-picker {}
                                    {}
                                    @(subscribe [::time-picker :not-initialized])
                                    #(dispatch [::set-max :not-initialized %])
                                    #(dispatch [::set-time-picker :not-initialized %])]
                                   [:input {:value @(subscribe [::time-picker :not-initialized])
                                            :readOnly true}]
                                   (html-edn (focus-on @app-db
                                                       [::time-picker :not-initialized]))])))

(defcard-rg time-picker-with-no-max
            "Time picker with no maximum"
            (fn [_ _]
              (reagent/as-element [:div
                                   [time-picker {}
                                    {:min 30
                                     :default 25
                                     :step 2
                                     :fast-step 11}
                                    @(subscribe [::time-picker :no-max])
                                    #(dispatch [::set-max :no-max %])
                                    #(dispatch [::set-time-picker :no-max %])]
                                   (html-edn (focus-on @app-db [::time-picker :no-max]))])))

(defcard-rg time-picker-with-dynamic-max
            "Time picker with dynamic maximum"
            (fn [_ _]
              (reagent/as-element [:div
                                   [time-picker {}
                                    {:min 30
                                     :default 25
                                     :step 2
                                     :fast-step 11}
                                    @(subscribe [::time-picker :dynamic-max])
                                    #(dispatch [::set-max :dynamic-max %])
                                    #(dispatch [::set-time-picker :dynamic-max %])
                                    (fn [{:keys [last]}] (when (>= last 200) 200))]
                                   (html-edn (focus-on @app-db [::time-picker :dynamic-max]))])))
