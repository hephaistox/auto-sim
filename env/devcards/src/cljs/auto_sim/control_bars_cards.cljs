(ns auto-sim.control-bars-cards
  (:require
   [auto-sim                   :as-alias sim]
   [auto-sim.control-bar       :refer [iteration-input simulation-control-bar]]
   [devcards.core              :refer [defcard-rg]]
   [devcards.util.edn-renderer :refer [html-edn]]
   [re-frame.db                :refer [app-db]]
   [reagent.core               :as reagent]))

(defcard-rg simulation-control-bar-card
            "### Simulation control bar"
            (fn [_ _]
              (reagent/as-element [:div
                                   (html-edn (-> @app-db
                                                 (select-keys [::sim/control-bar])
                                                 (update ::sim/control-bar
                                                         #(select-keys % [:iteration]))))
                                   [simulation-control-bar {}
                                    :en
                                    100]])))

(defcard-rg simulation-control-bar-with-no-opt
            "### Simulation control bar with no option"
            (fn [_ _] (reagent/as-element [:div [simulation-control-bar :en 100]])))

(defcard-rg linked-iteration-input
            "### Simulation control bar with no option"
            (reagent/as-element [iteration-input]))
