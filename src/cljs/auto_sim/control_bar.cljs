(ns auto-sim.control-bar
  "Components to manage a simulation"
  (:require
   [auto-web.page.builder :refer [merge-opts]]
   [re-frame.core         :refer [dispatch reg-event-fx reg-sub subscribe]]))

(def dic
  {:play {:fr "Lecture"
          :en "Play"}
   :stop {:fr "Stop"
          :en "Stop"}
   :pause {:fr "Pause"
           :en "Pause"}
   :backward {:fr "Retour"
              :en "Backward"}
   :forward {:fr "Avance"
             :en "Forward"}
   :fast-forward {:fr "Avance rapide"
                  :en "Fast forward"}
   :back-forward {:fr "Retour rapide"
                  :en "Fast backward"}})

(reg-sub ::control-iteration (fn [db _] (get-in db [::control-bar :iteration])))

(reg-event-fx ::stop (fn [{:keys [db]} _] {:db (assoc-in db [::control-bar :iteration] 0)}))

(reg-event-fx ::next-iteration
              (fn [{:keys [db]} _] {:db (update-in db [::control-bar :iteration] inc)}))

(reg-event-fx ::previous-iteration
              (fn [{:keys [db]} _] {:db (update-in db [::control-bar :iteration] dec)}))

(reg-event-fx ::set-iteration
              (fn [{:keys [db]} [_ it]] {:db (assoc-in db [::control-bar :iteration] it)}))

(defn- simulation-control-button
  [fa-icon tooltip on-click]
  [:i.w3-tooltip.w3-button.fa {:style {:overflow "visible"}
                               :on-click on-click
                               :class fa-icon}
   [:span.w3-text.w3-tag.w3-padding {:style {:position "absolute"
                                             :left "0px"
                                             :bottom "-2em"}}
    tooltip]])

(defn simulation-control-bar
  [opt & l]
  (let [opt (if (map? opt) opt {})
        l (if (map? opt) (first l) opt)
        tr #(get-in dic [% l])]
    [:div.w3-bar
     (merge-opts {:style {:align-items "center"
                          :overflow "visible"}}
                 opt)
     [simulation-control-button "fa-stop" (tr :stop) #(dispatch [::stop])]
     [simulation-control-button "fa-play" (tr :play) #(dispatch [::next-iteration])]
     [simulation-control-button "fa-fast-forward" (tr :fast-forward)]
     [simulation-control-button "fa-fast-backward" (tr :back-forward)]
     [simulation-control-button "fa-backward" (tr :backward) #(dispatch [::previous-iteration])]
     [simulation-control-button "fa-pause" (tr :pause)]
     [simulation-control-button "fa-forward" (tr :forward) #(dispatch [::next-iteration])]]))

(defn iteration-input
  []
  (let [control-iteration @(subscribe [::control-iteration])]
    [:input.w3-input.w3-border.w3-hover-grey.s3.w3-col
     {:type "number"
      :min "0"
      :on-change #(dispatch [::set-iteration [(.. % -target -value)]])
      :value control-iteration}]))
