(ns auto-sim.modal-cards
  (:require
   [auto-sim                   :as-alias sim]
   [auto-sim.modal             :refer [simulation-control-panel]]
   [devcards.core              :refer [defcard-rg]]
   [devcards.util.edn-renderer :refer [html-edn]]
   [re-frame.core              :refer [dispatch]]
   [re-frame.db                :refer [app-db]]
   [reagent.core               :as reagent]))

(defcard-rg simulation-control-panel-card
            "### Simulation control panel"
            (fn [_ _]
              (reagent/as-element [:div
                                   [:button {:on-click #(dispatch [::sim/unselect])}
                                    "Remove selection"]
                                   [:button {:on-click #(dispatch [::sim/select
                                                                   {:id 12
                                                                    :input ["p3" "p4" "p5"]
                                                                    :output ["p1" "p2"]}])}
                                    "Select an entity"]
                                   [simulation-control-panel]
                                   (html-edn (-> @app-db
                                                 (select-keys [::sim/modal])))])))
