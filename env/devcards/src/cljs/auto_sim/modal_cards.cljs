(ns auto-sim.modal-cards
  (:require
   [auto-sim.control-bar :as-alias cb]
   [auto-sim.modal       :as sut]
   [devcards.core        :refer [defcard-rg]]
   [re-frame.core        :refer [dispatch]]
   [re-frame.db          :refer [app-db]]
   [reagent.core         :as reagent]))

(defcard-rg simulation-control-panel
            "### Simulation control panel"
            (fn [_ _]
              (reagent/as-element [:div
                                   [:button {:on-click #(dispatch [::sut/select nil])}
                                    "Remove selection"]
                                   [:button {:on-click #(dispatch [::sut/select
                                                                   {:id 12
                                                                    :input ["p3" "p4" "p5"]
                                                                    :output ["p1" "p2"]}])}
                                    "Select"]
                                   [sut/simulation-control-panel]]))
            app-db
            {:inspect-data true})
