(ns auto-sim.component.speed-cards
  (:require
   [auto-sim.component.speed   :refer [time-speed]]
   [devcards.core              :refer [defcard-rg]]
   [devcards.util.edn-renderer :refer [html-edn]]
   [re-frame.core              :refer [dispatch reg-event-db reg-sub subscribe]]
   [re-frame.db                :refer [app-db]]
   [reagent.core               :as reagent]))

(reg-event-db ::set-speed (fn [db [_ k val]] (assoc-in db [:speed k] val)))

(reg-sub ::speed (fn [db [_ k]] (get-in db [:speed k])))

(defn- focus-on
  [m ks]
  (-> m
      (select-keys [:speed])
      (update :speed select-keys [ks])))

(defcard-rg time-speed-card
            "### Time speed
The default is 4"
            (fn [_ _]
              (reagent/as-element [:div
                                   [time-speed {}
                                    [0.5 1 4]
                                    @(subscribe [::speed :first-card])
                                    #(dispatch [::set-speed :first-card %])
                                    4]
                                   [:br]
                                   @(subscribe [::speed :first-card])
                                   (html-edn (focus-on @app-db :first-card))])))

(defcard-rg time-speed2-card
            "If value doesn't exist, it is defaulted to the first value."
            (fn [_ _]
              (reagent/as-element [:div
                                   [time-speed {}
                                    [0.5 1 4]
                                    @(subscribe [::speed :second-card])
                                    #(dispatch [::set-speed :second-card %])
                                    10]
                                   [:br]
                                   @(subscribe [::speed :second-card])
                                   (html-edn (focus-on @app-db :second-card))])))

(defcard-rg time-speed3-card
            "Share value but has no opts"
            (fn [_ _]
              (reagent/as-element [:div
                                   [time-speed
                                    [0.5 1 8]
                                    @(subscribe [::speed :third-card])
                                    #(dispatch [::set-speed :third-card %])]
                                   (html-edn (focus-on @app-db :third-card))])))

(defcard-rg time-speed3-card
            "Share value but has  opts"
            (fn [_ _]
              (reagent/as-element [time-speed {:class "w3-card"}
                                   [0.5 1 8]
                                   @(subscribe [::speed :third-card])
                                   #(dispatch [::set-speed :third-card %])])))
