(ns auto-sim.modal
  (:require
   [auto-sim.canvas]
   [auto-sim.control-bar :refer [iteration-input]]
   [clojure.string       :as str]
   [re-frame.core        :refer [dispatch reg-event-fx reg-sub subscribe]]))

(reg-sub ::modal-close? (fn [db _] (get-in db [::modal :close])))
(reg-event-fx ::toggle-close-modal
              (fn [{:keys [db]} [_ _it]] {:db (update-in db [::modal :close] not)}))

(reg-sub ::selected-id (fn [db _] (get-in db [::modal :selected-id])))
(reg-event-fx ::select (fn [{:keys [db]} [_ id]] {:db (assoc-in db [::modal :selected-id] id)}))

(defn opening-element
  [opts]
  [:div.w3-center (assoc opts :on-click #(dispatch [::toggle-close-modal])) [:i.fa.fa-angleup]])

(defn simulation-control-panel
  [opts]
  [:div.w3-display-content.w3-small
   opts
   [:div.w3-center {:style {:border "0.3em"
                            :background-color "#eee"
                            :border-radius "1em"
                            :cursor (if @(subscribe [::modal-close?]) "n-resize" "s-resize")}
                    :on-click #(dispatch [::toggle-close-modal])}
    [:i.fa.fa-circle.w3-tiny]]
   [:div.w3-row.w3-panel {:style {:display (if @(subscribe [::modal-close?]) "none" "block")}}
    [:p.s1.w3-col "Iteration"]
    [iteration-input]]
   (when-let [{:keys [id input output]} @(subscribe [::selected-id])]
     [:div.w3-row.w3-panel {:style {:display (if @(subscribe [::modal-close?]) "none" "block")}}
      [:p.s1.w3-col "Id"]
      [:p.s3.w3-col id]
      [:p.s1.w3-col "Input"]
      [:p.s3.w3-col (str/join ", " input)]
      [:p.s1.w3-col "Output"]
      [:p.s3.w3-col (str/join ", " output)]])])
