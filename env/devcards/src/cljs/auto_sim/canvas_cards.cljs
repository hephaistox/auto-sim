(ns auto-sim.canvas-cards
  (:require
   [auto-sim.canvas      :as sut]
   [auto-sim.control-bar :as-alias cb]
   [auto-sim.links       :refer [links]]
   [devcards.core        :refer [defcard-rg]]
   [re-frame.core        :refer [dispatch-sync]]
   [re-frame.db          :refer [app-db]]
   [reagent.core         :as reagent]))

(def rendering-data
  (->> [{:render-id :exemple-a
         :sprite :machine
         :x 0
         :y 0}
        {:render-id :exemple-c
         :sprite :product
         :x 50
         :y 10}
        {:render-id :exemple-d
         :sprite :product
         :x 10
         :y 20}
        {:render-id :exemple-b
         :sprite :product
         :x 10
         :y 10}]
       (mapv (fn [{:keys [sprite render-id x y]}]
               (let [{:keys [size-x size-y img]} (get sut/sprites sprite)]
                 [render-id {:render-id render-id
                             :img (:url (get links img))
                             :src-basis {:width size-x
                                         :height size-y
                                         :x x
                                         :y y}}])))
       (into {})))

(dispatch-sync [::sut/set-render-data rendering-data])

(defcard-rg simulation-control-panel-multi-select
            "### Simulation control panel with multi select mode"
            (fn [_ _]
              (reagent/as-element [:div
                                   [:div.w3-center [sut/control-bar]]
                                   [sut/layout {:style {:width "100%"
                                                        :height "500px"}}
                                    {:multi-select-mode? true}]]))
            app-db
            {:inspect-data true})

;; (defcard-rg simulation-control-panel-mono-select
;;             "### Simulation control panel with mono select mode"
;;             (fn [_ _]
;;               (reagent/as-element [sut/layout {:style {:width "1000px"
;;                                                        :height "500px"}}
;;                                    {:multi-select-mode? false}]))
;;             app-db
;;             {:inspect-data true})
