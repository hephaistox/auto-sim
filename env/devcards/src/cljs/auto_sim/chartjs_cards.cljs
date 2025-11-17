(ns auto-sim.chartjs-cards
  (:require
   [auto-sim.charts :refer [bar-chart bubble-chart doughnut-chart line-chart polar-chart]]
   [devcards.core   :refer [defcard-rg]]
   [re-frame.db     :refer [app-db]]
   [reagent.core    :as reagent]))

(defcard-rg bar-chart-card
            "### Bar chart"
            (fn [_ _]
              (reagent/as-element [bar-chart
                                   {:type "bar"
                                    :data {:labels ["Red" "Blue" "Yellow" "Green" "Purple" "Orange"]
                                           :datasets [{:label "# of Votes"
                                                       :data [12 19 3 5 2 3]
                                                       :borderWidth 1}]}
                                    :options {:scales {:y {:beginAtZero true}}
                                              :onClick (fn [e] (js/console.log e))
                                              :interaction {:mode "point"}
                                              :events ["click"]}}]))
            app-db)

(defcard-rg bubble-chart-card
            "### Bubble chart"
            (fn [_ _]
              (reagent/as-element [bubble-chart {:type "bubble"
                                                 :data {:datasets [{:label "First dataset"
                                                                    :data [{:x 20
                                                                            :y 30
                                                                            :r 15}
                                                                           {:x 40
                                                                            :y 10
                                                                            :r 10}]
                                                                    :backgroundColor
                                                                    "rgb(255, 99, 132)"}]}
                                                 :options {}}]))
            app-db)

(defcard-rg doughnut-card
            "### Doughnut chart"
            (fn [_ _]
              (reagent/as-element [doughnut-chart {:type "doughnut"
                                                   :labels ["Red" "Blue" "Yellow"]
                                                   :data {:datasets [{:label "My first dataset"
                                                                      :data [300 50 100]
                                                                      :backgroundColor
                                                                      ["rgb(255, 99, 132)"
                                                                       "rgb(54, 162, 235)"
                                                                       "rgb(255, 205, 86)"]
                                                                      :hoverOffset 4}]}
                                                   :options {}}]))
            app-db)

(defcard-rg line-card
            "### Line chart"
            (fn [_ _]
              (reagent/as-element [line-chart {:type "line"
                                               :data {:datasets [{:label "My first dataset"
                                                                  :data [65 59 80 81 56 55 40]
                                                                  :fill true
                                                                  :backgroundColor
                                                                  ["rgb(255, 99, 132)"
                                                                   "rgb(54, 162, 235)"
                                                                   "rgb(255, 205, 86)"]
                                                                  :borderColor "rgb(75, 192, 192)"
                                                                  :tension 0.1}]
                                                      :labels ["janvier" "fevrier"]}}]))
            app-db)

(defcard-rg polar-card
            "### Polar chart"
            (fn [_ _]
              (reagent/as-element [polar-chart {:type "polarArea"
                                                :data {:datasets [{:label "My first dataset"
                                                                   :data [65 59 80 81 56 55 40]
                                                                   :backgroundColor
                                                                   ["rgb(255, 99, 132)"
                                                                    "rgb(54, 162, 235)"
                                                                    "rgb(255, 205, 86)"]}]}}]))
            app-db)
