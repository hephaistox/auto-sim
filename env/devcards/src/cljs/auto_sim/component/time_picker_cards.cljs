(ns auto-sim.component.time-picker-cards
  (:require
   [auto-sim.component.time-picker :refer [get-time time-picker update-time]]
   [devcards.core                  :refer [defcard-rg]]
   [devcards.util.edn-renderer     :refer [html-edn]]
   [re-frame.core                  :refer [dispatch reg-event-db reg-sub subscribe]]
   [re-frame.db                    :refer [app-db]]
   [reagent.core                   :as reagent]))

(reg-event-db ::set-time (fn [db [_ k time _ _]] (update-in db [::time-picker k] update-time time)))

(reg-sub ::tp-vals (fn [db [_ k]] (get-in db [::time-picker k])))

(defn- focus-on
  [m path]
  (let [[a b] path]
    (-> m
        (select-keys [a])
        (update a select-keys [b]))))

(defcard-rg
 time-picker-card
 "### A time picker
Starts with default 35, step is 2, fast-step is 11. Min and max are known in advance: 30, 90"
 (fn [_ _]
   (let [tp-data {:min 30
                  :max 90
                  :time 35
                  :step 2
                  :fast-step 11}
         tp-vals @(subscribe [::tp-vals :std tp-data])]
     (reagent/as-element [:div
                          [time-picker {}
                           tp-data
                           tp-vals
                           #(dispatch [::set-time :std % tp-data tp-vals])]
                          [:input {:value (get-time tp-data @(subscribe [::tp-vals :std tp-data]))
                                   :read-only true}]
                          (html-edn (focus-on @app-db [::time-picker :std tp-data]))]))))

(defcard-rg time-picker-init-below-min
            "When default value is 25, it is below the minimum, it is defaulted to the minimum 30"
            (fn [_ _]
              (let [tp-data {:min 30
                             :max 90
                             :step 2
                             :time 25
                             :fast-step 11}]
                (reagent/as-element
                 [:div
                  [time-picker {}
                   tp-data
                   @(subscribe [::tp-vals :below-min tp-data])
                   #(dispatch [::set-time :below-min % tp-data])]
                  [:input {:value (get-time tp-data @(subscribe [::tp-vals :below-min tp-data]))
                           :read-only true}]
                  (html-edn (focus-on @app-db [::time-picker :below-min tp-data]))]))))

(defcard-rg time-picker-not-initialized
            "Time picker not initialized is suppose to start at 0, has no maximum"
            (fn [_ _]
              (let [tp-data {}]
                (reagent/as-element
                 [:div
                  [time-picker {}
                   tp-data
                   @(subscribe [::tp-vals :not-initialized tp-data])
                   #(dispatch [::set-time :not-initialized % tp-data])]
                  [:input {:value (get-time tp-data
                                            @(subscribe [::tp-vals :not-initialized tp-data]))
                           :read-only true}]
                  (html-edn (focus-on @app-db [::time-picker :not-initialized]))]))))

(defcard-rg time-picker-with-no-max
            "Time picker with no maximum"
            (fn [_ _]
              (let [tp-data {:min 30
                             :step 2
                             :fast-step 11}]
                (reagent/as-element [:div
                                     [time-picker {}
                                      tp-data
                                      @(subscribe [::tp-vals :no-max tp-data])
                                      #(dispatch [::set-time :no-max % tp-data])]
                                     (html-edn (focus-on @app-db [::time-picker :no-max]))]))))

(defcard-rg time-picker-with-dynamic-max
            "Time picker with dynamic maximum"
            (fn [_ _]
              (let [tp-data {:min 30
                             :time 25
                             :step 2
                             :fast-step 11}]
                (reagent/as-element [:div
                                     [time-picker {}
                                      tp-data
                                      @(subscribe [::tp-vals :dynamic-max tp-data])
                                      #(dispatch [::set-time :dynamic-max % tp-data])
                                      (fn [{:keys [time]}] (when (>= time 200) 200))]
                                     (html-edn (focus-on @app-db [::time-picker :dynamic-max]))]))))
