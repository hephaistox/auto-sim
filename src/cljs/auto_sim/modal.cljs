(ns auto-sim.modal
  (:require
   [auto-sim.canvas]
   [auto-sim.control-bar :refer [iteration-input]]
   [re-frame.core        :refer [dispatch reg-event-fx reg-sub subscribe]]))

(reg-sub ::modal-close? (fn [db _] (get-in db [::modal :close])))
(reg-event-fx ::close-modal (fn [{:keys [db]} [_ _it]] {:db (assoc-in db [::modal :close] true)}))

(defn simulation-control-panel
  [opt]
  [:div.w3-card.w3-display-content.w3-small
   (assoc-in opt [:style :display] (if @(subscribe [::modal-close?]) "none" "block"))
   [:i.fa.fa-close.w3-display-top-left.w3-button {:on-click #(dispatch [::close-modal])}]
   [:div.w3-row.w3-panel [:p.s1.w3-col "Iteration"] [iteration-input]]
   (when-let [{:keys [id input output]} @(subscribe [:auto-sim.canvas/icon-id-selected])]
     [:div.w3-row.w3-panel
      [:p.s1.w3-col "Id"]
      [:p.s3.w3-col id]
      [:p.s1.w3-col "Input"]
      [:p.s3.w3-col input]
      [:p.s1.w3-col "Output"]
      [:p.s3.w3-col output]])])
