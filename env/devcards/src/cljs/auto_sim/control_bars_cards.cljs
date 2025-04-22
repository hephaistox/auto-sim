(ns auto-sim.control-bars-cards
  (:require
   [auto-sim.control-bar :as sut]
   [devcards.core        :refer [defcard-rg]]
   [re-frame.db          :refer [app-db]]
   [reagent.core         :as reagent]))

(defcard-rg simulation-control-bar
            "### Simulation control bar"
            (fn [_ _]
              (reagent/as-element [sut/simulation-control-bar {}
                                   :en
                                   100]))
            app-db
            {:inspect-data true})

(defcard-rg simulation-control-bar-with-no-opt
            "### Simulation control bar with no option"
            (fn [_ _]
              (reagent/as-element [sut/simulation-control-bar {}
                                   :en
                                   100]))
            app-db
            {:inspect-data true})

(defcard-rg linked-iteration-input
            "### Simulation control bar with no option"
            (fn [_ _]
              (reagent/as-element [sut/iteration-input {}]))
            app-db
            {:inspect-data true})
