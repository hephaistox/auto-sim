(ns auto-sim.canvas-cards
  (:require
   [auto-sim                   :as-alias sim]
   [auto-sim.canvas            :as sut]
   [auto-sim.control-bar       :as-alias cb]
   [auto-sim.links             :refer [links]]
   [devcards.core              :refer [defcard-rg]]
   [devcards.util.edn-renderer :refer [html-edn]]
   [re-frame.core              :refer [dispatch]]
   [re-frame.db                :refer [app-db]]
   [reagent.core               :as reagent]))

(defn- focus-on
  [m path]
  (let [[a b] path]
    (-> m
        (select-keys [a])
        (update a select-keys [b]))))

(def sprites
  (->> {:source {:size-x 2
                 :size-y 2
                 :img ::sim/source}
        :sink {:size-x 2
               :size-y 2
               :img ::sim/sink}
        :machine {:size-x 4
                  :size-y 4
                  :img ::sim/machine}
        :product {:size-x 2
                  :size-y 2
                  :img ::sim/product}}
       (map (fn [[k v]] [k (assoc v :sprite-id k)]))
       (into {})))

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
               (let [{:keys [size-x size-y img]} (get sprites sprite)]
                 [render-id {:render-id render-id
                             :img (:url (get links img))
                             :render-items-basis {:width size-x
                                                  :height size-y
                                                  :x x
                                                  :y y}}])))
       (into {})))

(dispatch [::sut/set-render-items :canvas-1 rendering-data])

(defcard-rg
 simulation-control-panel-multi-select
 "### Simulation control panel with multi select mode"
 (fn [_ _]
   (-> [:div
        [:div.w3-center
         [:div.w3-bar.w3-padding-16 [sut/bn-autofit :canvas-1] [sut/bn-drag-move :canvas-1]]]
        [sut/layout* {:style {:width "100%"
                              :height "500px"}}
         :canvas-1
         {:multi-select-mode? true}]
        (html-edn (focus-on @app-db [::sut/canvas :canvas-1]))]
       reagent/as-element)))

#_(defcard-rg
   simulation-control-panel-mono-select
   "### Simulation control panel with mono select mode"
   (fn [_ _]
     (-> [:div
          [:button {:on-click (fn [_]
                                (dispatch-sync [::sut/set-render-items :canvas-2 rendering-data]))}
           "Set data"]
          [:div.w3-center [:div.bar [sut/bn-autofit :canvas-1] [sut/bn-arrows :canvas-1]]]
          [sut/layout {:style {:width "1000px"
                               :height "500px"}}
           :canvas-2
           {:multi-select-mode? false}
           rendering-data]
          (html-edn (focus-on @app-db [::sut/canvas :canvas-2]))]
         reagent/as-element)))
