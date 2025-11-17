(ns auto-sim.control-bar
  "Components to manage a simulation"
  (:require
   [auto-sim              :as-alias sim]
   [auto-web.page.builder :refer [merge-opts]]
   [re-frame.core         :refer [dispatch reg-event-fx reg-sub subscribe]]))

(def dic
  {:fast-backward {:fr "Retour rapide"
                   :en "Fast backward"}
   :backward {:fr "Retour"
              :en "Backward"}
   :play {:fr "Lecture"
          :en "Play"}
   :stop {:fr "Stop"
          :en "Stop"}
   :pause {:fr "Pause"
           :en "Pause"}
   :forward {:fr "Avance"
             :en "Forward"}
   :fast-forward {:fr "Avance rapide"
                  :en "Fast forward"}})

(reg-sub ::sim/control-iteration (fn [db _] (get-in db [::sim/control-bar :iteration])))

(reg-event-fx ::sim/stop-iteration
              (fn [{:keys [db]} _] {:db (assoc-in db [::sim/control-bar :iteration] 0)}))

(reg-event-fx ::sim/next-iteration
              (fn [{:keys [db]} [_ m]]
                (let [it (get-in db [::sim/control-bar :iteration])]
                  {:db (cond-> db
                         (or (nil? m) (< it (dec m))) (assoc-in [::sim/control-bar :iteration]
                                                       (inc it)))})))

(reg-event-fx ::sim/previous-iteration
              (fn [{:keys [db]} _] {:db (update-in db [::sim/control-bar :iteration] dec)}))

(reg-event-fx ::sim/set-iteration
              (fn [{:keys [db]} [_ it]] {:db (assoc-in db [::sim/control-bar :iteration] it)}))

(defn- simulation-control-button
  [opts fa-icon tooltip on-click]
  [:div.w3-tooltip.w3-cell {:style {:overflow "visible"}}
   [:button.w3-button.fa
    (merge-opts {:style {:overflow "visible"}
                 :on-click on-click
                 :class fa-icon}
                opts)]
   [:div.w3-text.w3-tag.w3-padding {:style {:position "absolute"
                                            :left "0px"
                                            :bottom "-2em"}}
    tooltip]])

(defn simulation-control-bar
  [opts l max-iteration]
  (let [opts* (if (nil? max-iteration) {} opts)
        l* (if (nil? max-iteration) opts l)
        max-iteration* (if (nil? max-iteration) l max-iteration)
        tr #(get-in dic [% l*])
        disabled {:style {:cursor "default"}
                  :disabled true}
        iteration (or @(subscribe [::sim/control-iteration]) 0)]
    (when (< iteration 0) (dispatch [::sim/set-iteration 0]))
    (when (>= iteration max-iteration) (dispatch [::sim/set-iteration (dec max-iteration)]))
    [:div
     (merge-opts {:style {:align-items "center"
                          :overflow "visible"}}
                 opts*)
     [simulation-control-button
      (when (= iteration 0) disabled)
      "fa-fast-backward"
      (tr :fast-backward)]
     [simulation-control-button
      (when (= iteration 0) disabled)
      "fa-backward"
      (tr :backward)
      #(dispatch [::sim/previous-iteration])]
     [simulation-control-button
      (when (= iteration (dec max-iteration*)) disabled)
      "fa-play"
      (tr :play)
      #(dispatch [::sim/next-iteration max-iteration])]
     [simulation-control-button {}
      "fa-pause"
      (tr :pause)]
     [simulation-control-button
      (when (= iteration 0) disabled)
      "fa-stop"
      (tr :stop)
      #(dispatch [::sim/stop-iteration])]
     [simulation-control-button
      (when (= iteration (dec max-iteration*)) disabled)
      "fa-forward"
      (tr :forward)
      #(dispatch [::sim/next-iteration max-iteration])]
     [simulation-control-button
      (when (= iteration (dec max-iteration*)) disabled)
      "fa-fast-forward"
      (tr :fast-forward)]]))

(defn iteration-input
  [opts]
  (let [control-iteration @(subscribe [::sim/control-iteration])]
    [:input.w3-input.w3-border.w3-hover-grey.s3.w3-col
     (assoc opts
            :type "number"
            :min "0"
            :on-change #(dispatch [::sim/set-iteration (js/parseInt (.. % -target -value))])
            :value control-iteration)]))
