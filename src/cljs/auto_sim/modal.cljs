(ns auto-sim.modal
  "Modal window to show"
  (:require
   [auto-sim             :as-alias sim]
   [auto-sim.control-bar :refer [iteration-input]]
   [clojure.string       :as str]
   [re-frame.core        :refer [dispatch reg-event-db reg-sub subscribe]]))

(reg-sub ::sim/modal-close? (fn [db _] (get-in db [::sim/modal :close])))

(reg-event-db ::sim/toggle-close-modal (fn [db _] (update-in db [::sim/modal :close] not)))

(reg-sub ::sim/selected-id (fn [db _] (get-in db [::sim/modal :selected-id])))

(reg-event-db ::sim/select (fn [db [_ id]] (assoc-in db [::sim/modal :selected-id] id)))

(reg-event-db ::sim/unselect (fn [db _] (update db ::sim/modal dissoc :selected-id)))

(defn opening-element
  [opts]
  [:div.w3-center (assoc opts :on-click #(dispatch [::sim/toggle-close-modal])) [:i.fa.fa-angleup]])

(defn simulation-control-panel
  [opts]
  (let [modal-close? @(subscribe [::sim/modal-close?])]
    [:div.w3-display-content.w3-small
     opts
     [:div.w3-center {:style {:border "0.3em"
                              :background-color "#eee"
                              :border-radius "1em"
                              :cursor (if modal-close? "n-resize" "s-resize")}
                      :on-click #(dispatch [::sim/toggle-close-modal])}
      [:i.fa.fa-circle.w3-tiny]]
     [:div.w3-row.w3-panel {:style {:display (if modal-close? "none" "block")}}
      [:p.s1.w3-col "Iteration"]
      [iteration-input]]
     (when-let [{:keys [id input output]} @(subscribe [::sim/selected-id])]
       [:div.w3-row.w3-panel {:style {:display (if modal-close? "none" "block")}}
        [:p.s1.w3-col "Id"]
        [:p.s3.w3-col id]
        [:p.s1.w3-col "Input"]
        [:p.s3.w3-col (str/join ", " input)]
        [:p.s1.w3-col "Output"]
        [:p.s3.w3-col (str/join ", " output)]])]))
