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
   :fast-forward {:fr "Avance rapide"
                  :en "Fast forward"}
   :back-forward {:fr "Retour rapide"
                  :en "Fast backward"}})

(reg-sub ::control-iteration (fn [db _] (get-in db [::control-bar :iteration])))

(reg-event-fx ::stop (fn [{:keys [db]} _] {:db (assoc-in db [::control-bar :iteration] 0)}))

(reg-event-fx ::next-iteration
              (fn [{:keys [db]} [_ m]]
                (let [it (get-in db [::control-bar :iteration])]
                  {:db (cond-> db
                         (or (nil? m) (< it (dec m))) (assoc-in [::control-bar :iteration]
                                                       (inc it)))})))

(reg-event-fx ::previous-iteration
              (fn [{:keys [db]} _] {:db (update-in db [::control-bar :iteration] dec)}))

(reg-event-fx ::set-iteration
              (fn [{:keys [db]} [_ it]] {:db (assoc-in db [::control-bar :iteration] it)}))

(defn- simulation-control-button
  [opts fa-icon tooltip on-click]
  [:button.w3-tooltip.w3-button.fa
   (merge-opts {:style {:overflow "visible"}
                :on-click on-click
                :class fa-icon}
               opts)
   [:span.w3-text.w3-tag.w3-padding {:style {:position "absolute"
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
        iteration (or @(subscribe [::control-iteration]) 0)]
    (when (< iteration 0) (dispatch [::set-iteration 0]))
    (when (>= iteration max-iteration) (dispatch [::set-iteration (dec max-iteration)]))
    [:div
     (merge-opts {:style {:align-items "center"
                          :overflow "visible"}}
                 opts*)
     [simulation-control-button
      (when (= iteration 0) disabled)
      "fa-stop"
      (tr :stop)
      #(dispatch [::stop])]
     [simulation-control-button
      (when (= iteration (dec max-iteration*)) disabled)
      "fa-play"
      (tr :play)
      #(dispatch [::next-iteration max-iteration])]
     [simulation-control-button
      (when (= iteration (dec max-iteration*)) disabled)
      "fa-fast-forward"
      (tr :fast-forward)]
     [simulation-control-button
      (when (= iteration 0) disabled)
      "fa-fast-backward"
      (tr :back-forward)]
     [simulation-control-button
      (when (= iteration 0) disabled)
      "fa-backward"
      (tr :backward)
      #(dispatch [::previous-iteration])]
     [simulation-control-button {}
      "fa-pause"
      (tr :pause)]
     [simulation-control-button
      (when (= iteration (dec max-iteration*)) disabled)
      "fa-forward"
      (tr :forward)
      #(dispatch [::next-iteration max-iteration])]]))

(defn iteration-input
  [opts]
  (let [control-iteration @(subscribe [::control-iteration])]
    [:input.w3-input.w3-border.w3-hover-grey.s3.w3-col
     (assoc opts
            :type "number"
            :min "0"
            :on-change #(dispatch [::set-iteration (js/parseInt (.. % -target -value))])
            :value control-iteration)]))
