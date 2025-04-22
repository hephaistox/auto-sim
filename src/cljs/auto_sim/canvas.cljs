(ns auto-sim.canvas
  (:require
   [auto-sim      :as-alias sim]
   [re-frame.core :refer [dispatch reg-event-db reg-sub subscribe]]
   [reagent.core  :as r]))

;; ********************************************************************************
;; Observe a dom element size

(defn- on-elt-resize
  [element on-resize]
  (let [rect (.-contentRect element)]
    (on-resize {:width (js/Math.round (.-width rect))
                :height (js/Math.round (.-height rect))})))

(defn- observe-element-size
  [canvas-id on-resize]
  (when-let [el (.getElementById js/document canvas-id)]
    (let [observer (js/ResizeObserver. (fn [entries _]
                                         (doseq [entry entries] (on-elt-resize entry on-resize))))]
      (.observe observer el)
      observer)))

;; ********************************************************************************
;; Icon selection

(reg-event-db ::select-icon
              (fn [db [_ render-id multi-select-key? multi-select-mode?]]
                (let [multi-select? (and multi-select-mode? multi-select-key?)]
                  (cond-> db
                    multi-select?
                    (update-in [::canvas :render-items] update-vals #(dissoc % :item-selected?))
                    :else (update-in [::canvas :render-items render-id :item-selected?] not)))))

(reg-event-db
 ::clear-selection
 (fn [db _]
   (update-in db [::canvas :render-items] update-vals (fn [x] (assoc x :item-selected? false)))))

;; ********************************************************************************
;; Render-items

(reg-sub ::render-items (fn [db _] (get-in db [::canvas :render-items])))

(defn- set-render-items [db render-items] (assoc-in db [::canvas :render-items] render-items))

(reg-event-db ::render-item-move
              (fn [db [_ render-id x y]]
                (update-in db
                           [::canvas :render-items render-id :src-basis]
                           assoc
                           :x (/ (js/Math.round (* 1000 x)) 1000)
                           :y (/ (js/Math.round (* 1000 y)) 1000))))

(defn- transform-render-item
  "Transform `render-item` from `source` to `destination` basis."
  [translation render-item]
  (let [{:keys [src-basis]} render-item
        {:keys [origin-x origin-y sd-x sd-y]} translation
        {:keys [width height x y]} src-basis]
    (assoc render-item
           :target-basis
           {:width (js/Math.round (* sd-x width))
            :height (js/Math.round (* sd-y height))
            :x (js/Math.round (+ origin-x (* sd-x x)))
            :y (js/Math.round (+ origin-y (* sd-y y)))})))

;; ********************************************************************************
;; Transformation

(defn- create-translation
  "A translation from `source` to `target` is computed"
  [db]
  (let [basis (get-in db [::canvas :basis])
        {source-width :width
         source-height :height}
        (:src basis)
        {origin-x-target :origin-x
         origin-y-target :origin-y
         target-width :width
         target-height :height}
        (:target basis)
        sd-x (/ target-width source-width)
        sd-y (/ target-height source-height)]
    (cond-> db
      (some? target-height) (update-in [::canvas :basis]
                                       assoc
                                       :translation
                                       {:origin-x origin-x-target
                                        :origin-y origin-y-target
                                        :sd-x sd-x
                                        :sd-y sd-y}))))

(defn- set-src-basis [db src-basis] (assoc-in db [::canvas :basis :src] src-basis))

(reg-sub ::basis-translation (fn [db _] (get-in db [::canvas :basis :translation])))

;; ********************************************************************************
;; Dragging mode

(reg-sub ::drag-mode? (fn [db _] (= :dragging (get-in db [::canvas :mode]))))

(reg-event-db ::toggle-drag-mode
              (fn [db _]
                (if (= :dragging (get-in db [::canvas :mode]))
                  (update db ::canvas dissoc :mode)
                  (assoc-in db [::canvas :mode] :dragging))))

;; ********************************************************************************
;; Target basis

(defn- on-target-basis-change
  "Called when the target basis has changed"
  [db]
  (let [translation (get-in db [::canvas :basis :translation])]
    (-> db
        create-translation
        (update-in [::canvas :render-items]
                   update-vals
                   (partial transform-render-item translation)))))

(reg-event-db ::size-target-basis
              (fn [db [_ target-basis]]
                (-> (update-in db
                               [::canvas :basis :target]
                               assoc
                               :width (:width target-basis)
                               :height (:height target-basis))
                    on-target-basis-change)))

(reg-event-db ::move-target-basis
              (fn [db [_ x y]]
                (-> (update-in db [::canvas :basis :target] assoc :origin-x x :origin-y y)
                    on-target-basis-change)))

(reg-event-db ::resize-target
              (fn [db [_ el-id]]
                (let [rect (.getBoundingClientRect el-id)
                      width (.-width rect)
                      height (.-height rect)]
                  (-> db
                      (update-in [::canvas :basis :target]
                                 assoc
                                 :origin-x 0
                                 :origin-y 0
                                 :width width
                                 :height height)
                      on-target-basis-change))))
(reg-event-db ::auto-fit
              (fn [db _]
                (-> db
                    (update-in [::canvas :basis :target] assoc :origin-x 0 :origin-y 0)
                    on-target-basis-change)))

;;;; ********************************************************************************
;; Drag icon

(reg-sub ::drag-icon-data (fn [db _] (get-in db [::canvas :drag-icon-data])))

(reg-event-db ::drag-icon-start
              (fn [db [_ drag-icon-start]]
                (assoc-in db [::canvas :drag-icon-data] drag-icon-start)))

(reg-event-db ::drag-icon-end (fn [db _] (update db ::canvas dissoc :drag-icon-data)))

(defn- on-drag-icon-mouse-move
  [drag-icon-data translation]
  (let [{:keys [sd-x sd-y origin-x origin-y]} translation
        {:keys [parent render-id pos-in-x pos-in-y]} drag-icon-data
        rect (.getBoundingClientRect parent)]
    (fn [e]
      (dispatch [::render-item-move
                 render-id
                 (/ (- (.-clientX e) (.-x rect) pos-in-x origin-x) sd-x)
                 (/ (- (.-clientY e) (.-y rect) pos-in-y origin-y) sd-y)]))))

(defn- on-drag-icon-mouse-down
  [root-parent-id render-id]
  (fn [e]
    (let [p (.getElementById js/document root-parent-id)
          target (.-target e)
          rect (.getBoundingClientRect target)
          client-x (.-clientX e)
          client-y (.-clientY e)
          pos-in-x (- client-x (.-x rect))
          pos-in-y (- client-y (.-y rect))]
      (dispatch [::drag-icon-start {:render-id render-id
                                    :parent p
                                    :pos-in-x pos-in-x
                                    :client-x client-x
                                    :client-y client-y
                                    :pos-in-y pos-in-y}]))))

;; ********************************************************************************
;; Drag basis

(reg-sub ::drag-basis-data (fn [db _] (get-in db [::canvas :drag-basis-data])))

(reg-event-db ::drag-basis-start
              (fn [db [_ m]]
                (-> db
                    (assoc-in [::canvas :drag-basis-data] m))))

(reg-event-db ::drag-basis-end
              (fn [db _]
                (-> db
                    (update ::canvas dissoc :drag-basis-data))))

(defn- on-drag-basis-mouse-move
  [drag-basis-data translation]
  (fn [e]
    (let [{:keys [parent pos-in-x pos-in-y]} drag-basis-data
          rect (.getBoundingClientRect parent)]
      (dispatch [::move-target-basis
                 (+ (- (.-clientX e) (.-x rect) pos-in-x))
                 (+ (- (.-clientY e) (.-y rect) pos-in-y))]))))

(defn- on-drag-basis-mouse-down
  [root-parent-id]
  (fn [e]
    (let [p (.getElementById js/document root-parent-id)
          target (.-target e)
          rect (.getBoundingClientRect target)
          client-x (.-clientX e)
          client-y (.-clientY e)
          pos-in-x (- client-x (.-x rect))
          pos-in-y (- client-y (.-y rect))
          m {:parent p
             :pos-in-x pos-in-x
             :client-x client-x
             :client-y client-y
             :pos-in-y pos-in-y}]
      (dispatch [::drag-basis-start m]))))

;; ********************************************************************************

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

(defn- icon
  [opts render-item]
  (let [{:keys [img target-basis item-selected?]} render-item
        {:keys [x y width height]} target-basis]
    [:img.w3-hover-shadow
     (-> opts
         (assoc :draggable false :src img)
         (update
          :style
          #(cond->
             (assoc % :height height :width width :border (when item-selected? "0.2em solid black"))
             (or x y) (assoc :position "absolute" :top y :left x))))]))

(defn- bounding-box
  [render-items]
  (let [res (reduce (fn [res render-item]
                      (let [{:keys [min-x max-x min-y max-y]} res
                            {:keys [x y width height]} (:src-basis render-item)]
                        {:min-x (min min-x x)
                         :min-y (min min-y y)
                         :max-x (max max-x (+ x width))
                         :max-y (max max-y (+ y height))}))
                    (let [{:keys [x y width height]
                           :or {x 0
                                y 0}}
                          (:src-basis (second (first render-items)))]
                      {:min-x x
                       :max-x (+ x width)
                       :min-y y
                       :max-y (+ y height)})
                    (rest (vals render-items)))
        {:keys [min-x min-y max-x max-y]} res]
    (assoc res :width (- max-x min-x) :height (- max-y min-y))))

;; ********************************************************************************
;; Public API

(reg-event-db ::set-render-data
              (fn [db [_ render-items]]
                (let [translation (get-in db [::canvas :basis :translation])]
                  (-> db
                      (set-render-items render-items)
                      (set-src-basis (bounding-box render-items))
                      (update-in [::canvas :render-items]
                                 update-vals
                                 (partial transform-render-item translation))))))

(defn control-bar
  []
  [:div.bar
   [:button.w3-btn {:on-click #(dispatch [::auto-fit])}
    [:i.fa.fa-expand]]
   (let [dragging-mode? @(subscribe [::drag-mode?])]
     [:button.w3-btn {:on-click #(do (dispatch [::toggle-drag-mode]) (dispatch [::drag-icon-end]))
                      :style (when dragging-mode? {:box-shadow "3px 6px #888888"})}
      [:i.fa.fa-arrows]])])

(defn layout
  [opts {:keys [multi-select-mode?]}]
  (let [canvas-id (random-uuid)]
    (r/create-class
     {:display-name "layout"
      :component-did-mount
      (fn [_] (let [el (.getElementById js/document canvas-id)] (dispatch [::resize-target el])))
      :reagent-render
      (fn []
        (let [drag-icon-data @(subscribe [::drag-icon-data])
              drag-basis-data @(subscribe [::drag-basis-data])
              translation @(subscribe [::basis-translation])
              dragging-mode? @(subscribe [::drag-mode?])
              render-items @(subscribe [::render-items])]
          (observe-element-size canvas-id #(dispatch [::size-target-basis %]))
          [:div
           (-> opts
               (update :style assoc
                       :position "relative"
                       :overflow "hidden"
                       :cursor (when dragging-mode? "move"))
               (assoc :on-mouse-move
                      (cond
                        drag-icon-data (on-drag-icon-mouse-move drag-icon-data translation)
                        drag-basis-data (on-drag-basis-mouse-move drag-basis-data translation))
                      :id canvas-id
                      :on-click #(dispatch [::clear-selection])
                      :on-mouse-down (when dragging-mode? (on-drag-basis-mouse-down canvas-id))
                      :on-mouse-leave #(do (dispatch [::clear-selection])
                                           (dispatch [::drag-icon-end])
                                           (dispatch [::drag-basis-end]))
                      :on-mouse-up #(do (when drag-icon-data (dispatch [::drag-icon-end]))
                                        (when drag-basis-data (dispatch [::drag-basis-end])))))
           (mapcat (fn [{:keys [render-id]
                         :as render-item}]
                     [^{:key render-id}
                      [icon {:on-click
                             #(do (.stopPropagation %)
                                  (dispatch
                                   [::select-icon render-id (.-metaKey %) multi-select-mode?]))
                             :on-mouse-down #(do (.stopPropagation %)
                                                 (when dragging-mode?
                                                   ((on-drag-icon-mouse-down canvas-id render-id)
                                                    %)))}
                       render-item]])
            (vals render-items))]))
      :component-will-unmount
      (fn [this] (when-let [observer (:observer @(r/state-atom this))] (.disconnect observer)))})))
