(ns auto-sim.canvas
  (:require
   [auto-sim              :as-alias sim]
   [auto-web.page.builder :refer [merge-opts]]
   [re-frame.core         :refer [reg-event-db reg-sub subscribe]]))

(reg-sub ::icon-id-selected (fn [db _] (get-in db [::canvas ::selected?])))

(reg-event-db ::select-icon
              (fn [db [_ icon]]
                (let [selected-icon-id (get-in db [::canvas ::selected?])]
                  (-> db
                      (assoc-in [::canvas ::selected?] (if (= icon selected-icon-id) nil icon))))))

(def sprites
  (->> {:source {:size "2em"
                 :img ::sim/source}
        :sink {:size "2em"
               :img ::sim/sink}
        :machine {:size "4em"
                  :img ::sim/machine}
        :product {:size "2em"
                  :img ::sim/product}}
       (map (fn [[k v]] [k (assoc v :sprite-id k)]))
       (into {})))

(defn- icon
  [opts left top k sprite]
  (let [{:keys [size img]} sprite]
    [:img
     (merge-opts opts
                 {:style (cond-> {:height size
                                  :border (when (= @(subscribe [::icon-id-selected]) k)
                                            "0.2em solid black")
                                  :width size}
                           (or top left) (assoc :position "absolute" :top top :left left))
                  :draggable false
                  :alt (:alt img)
                  :src (:url img)})]))

(defn layout
  [rendering-data sprites links]
  [:div.w3-row {:style {:height "100%"
                        :position "relative"}}
   #_(mapcat (fn [{:keys [rendering-id x y sprite input output]}]
               [^{:key rendering-id}
                [icon {:on-click #(dispatch [::select-icon rendering-id])}
                 (apply str x)
                 (apply str y)
                 rendering-id
                 (update (get sprites sprite) :img links)]
                (when (seq output)
                  (reduce conj
                          [:div.w3-flex.w3-border {:style {:position "relative"
                                                           :left "-10px"
                                                           :top "0px"}}]
                          (mapv (fn [output-product]
                                  (let [rendering-id (str rendering-id "/" output-product)]
                                    ^{:keys rendering-id}
                                    [icon {:on-click #(dispatch [::select-icon rendering-id])
                                           :style {:flex-direction "row"
                                                   :flex-wrap "wrap"}}
                                     nil
                                     nil
                                     rendering-id
                                     (update (:product sprites) :img links)]))
                                output)))
                (when (seq input)
                  (reduce conj
                          [:div.w3-flex.w3-border {:style {:position "relative"
                                                           :left "-10px"
                                                           :top "10px"}}]
                          (mapv (fn [input-product]
                                  (let [rendering-id (str rendering-id "/" input-product)]
                                    ^{:keys rendering-id}
                                    [icon {:on-click #(dispatch [::select-icon rendering-id])}
                                     nil
                                     nil
                                     rendering-id
                                     (update (:product sprites) :img links)]))
                                input)))])
      (vals rendering-data))])
