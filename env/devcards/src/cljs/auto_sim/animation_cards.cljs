(ns auto-sim.animation-cards
  (:require
   [auto-sim                       :as-alias sim]
   [auto-sim.animation             :refer [trajectory]]
   [auto-sim.component.time-picker :refer [time-picker]]
   [devcards.core                  :refer [defcard-rg]]
   [devcards.util.edn-renderer     :refer [html-edn]]
   [re-frame.core                  :refer [dispatch dispatch-sync subscribe]]
   [re-frame.db                    :refer [app-db]]))

;; (defn- box-example
;;   [pos class-color]
;;   [:div {:style {:left (str (:x pos) "px")
;;                  :top (str (:y pos) "px")
;;                  :height "10px"
;;                  :width "10px"
;;                  :position "absolute"}
;;          :class class-color}])

;; (dispatch-sync [::sim/time-picker-init
;;                 :animation
;;                 {:min 10
;;                  :max 96
;;                  :default 35
;;                  :step 2
;;                  :fast-step 11}])

;; (defcard-rg trajectory-card
;;             "## Two black boxes animated with a time-picker"
;;             (fn [_ _]
;;               (let [time @(subscribe [::sim/time-picker :animation])
;;                     src-pos {:x 5
;;                              :y 5
;;                              :t 13}
;;                     dst-pos {:x 95
;;                              :y 55
;;                              :t 30}
;;                     src-pos2 {:x 5
;;                               :y 5
;;                               :t 30}
;;                     dst-pos2 {:x 5
;;                               :y 35
;;                               :t 90}]
;;                 [:div
;;                  [:div.w3-row
;;                   [time-picker {:class "w3-half"}
;;                    :animation]
;;                   [:input.w3-half {:value @(subscribe [::sim/time-picker :animation])
;;                                    :readOnly true}]]
;;                  [:button {:on-click #(dispatch [::sim/start :trajectory-test 20])}
;;                   "Start"]
;;                  [:button {:on-click #(dispatch [::sim/stop :trajectory-test])}
;;                   "Stop"]
;;                  [:div {:style {:width "100px "
;;                                 :height "100px"
;;                                 :position "relative"}}
;;                   (box-example src-pos "w3-grey")
;;                   (box-example (trajectory time src-pos dst-pos) "w3-black")
;;                   (box-example (trajectory time src-pos2 dst-pos2) "w3-black")
;;                   (box-example dst-pos "w3-grey")]
;;                  [:div
;;                   [html-edn
;;                    (-> @app-db
;;                        (select-keys [::sim/time-picker])
;;                        (update ::sim/time-picker #(select-keys % [:animation])))]]])))
