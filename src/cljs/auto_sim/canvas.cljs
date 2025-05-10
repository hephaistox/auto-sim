(ns auto-sim.canvas
  "Canvas present render-items:

  - it can be graphically positionned
  - save the state in the local storage
  - it represents graphically the simulation state"
  (:require
   [auto-sim               :as-alias sim]
   [auto-web.local-storage :as-alias ls
                           :refer    [get-ls set-ls!]]
   [goog.object]
   [re-frame.core          :refer [dispatch
                                   inject-cofx
                                   reg-cofx
                                   reg-event-db
                                   reg-event-fx
                                   reg-fx
                                   reg-sub
                                   subscribe]]
   [reagent.core           :as r]))

; ********************************************************************************
;; Local storage saving

(reg-cofx :local-storage (fn [cofx] (assoc cofx :canvas-from-ls (get-ls ::canvas))))

(reg-event-fx ::load-from-ls
              [(inject-cofx :local-storage)]
              (fn [cofx _]
                {:db (:db cofx)
                 :fx [[:dispatch [::set-all-canvas (:canvas-from-ls cofx)]]]}))

(reg-fx :set-ls! (fn [value] (set-ls! ::canvas value)))

(reg-event-fx ::save-to-ls
              (fn [cofx _]
                (let [db (:db cofx)
                      canvas (get-in db [::canvas])]
                  {:fx [[:set-ls! canvas]]
                   :db db})))

;; ********************************************************************************
;; Render-element selection

(reg-event-db
 ::select-render-element
 (fn [db [_ canvas-id render-id multi-select-key? multi-select-mode?]]
   (let [multi-select? (and multi-select-mode? multi-select-key?)]
     (cond-> db
       (not multi-select?)
       (update-in [::canvas canvas-id :render-items] update-vals #(dissoc % :item-selected?))
       :else (update-in [::canvas canvas-id :render-items render-id :item-selected?] not)))))


(defn one-selected?
  "Returns true if at least one element is selected."
  [render-items]
  (->> render-items
       vals
       (filter :item-selected?)
       seq))

(reg-event-db ::clear-selection
              (fn [db [_ canvas-id]]
                (update-in db
                           [::canvas canvas-id :render-items]
                           update-vals
                           (fn [x] (assoc x :item-selected? false)))))

;; ********************************************************************************
;; Render-items

(reg-sub ::render-items (fn [db [_ canvas-id]] (get-in db [::canvas canvas-id :render-items])))

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

(reg-event-db ::render-item-move
              (fn [db [_ canvas-id render-id x y]]
                (let [translation (get-in db [::canvas canvas-id :basis :translation])]
                  (-> db
                      (update-in [::canvas canvas-id :render-items render-id]
                                 (fn [render-item]
                                   (let [render-item*
                                         (update render-item :src-basis assoc :x x :y y)]
                                     (transform-render-item translation render-item*))))))))

;; ********************************************************************************
;; Basis translation

(defn- create-translation
  "A translation from `source` to `target` is computed"
  [db canvas-id]
  (let [basis (get-in db [::canvas canvas-id :basis])
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
    (when (some? target-height)
      {:origin-x origin-x-target
       :origin-y origin-y-target
       :sd-x sd-x
       :sd-y sd-y})))

(reg-sub ::basis-translation
         (fn [db [_ canvas-id]] (get-in db [::canvas canvas-id :basis :translation])))

;; ********************************************************************************
;; Dragging mode

(reg-sub ::drag-mode? (fn [db [_ canvas-id]] (= :dragging (get-in db [::canvas canvas-id :mode]))))

(reg-event-db ::toggle-drag-mode
              (fn [db [_ canvas-id]]
                (if (= :dragging (get-in db [::canvas canvas-id :mode]))
                  (update-in db [::canvas canvas-id] dissoc :mode)
                  (assoc-in db [::canvas canvas-id :mode] :dragging))))

;; ********************************************************************************
;; Target basis

(defn- on-target-basis-change
  "To call when the target basis has changed"
  [db canvas-id]
  (let [translation (create-translation db canvas-id)]
    (update-in
     db
     [::canvas canvas-id]
     (fn [canva]
       (-> canva
           (assoc-in [:basis :translation] translation)
           (update :render-items update-vals (partial transform-render-item translation)))))))

(reg-event-db ::size-target-basis
              (fn [db [_ canvas-id target-basis]]
                (-> (update-in db
                               [::canvas canvas-id :basis :target]
                               assoc
                               :origin-x 0
                               :origin-y 0
                               :width (:width target-basis)
                               :height (:height target-basis))
                    (on-target-basis-change canvas-id))))

(reg-event-db ::move-target-basis
              (fn [db [_ canvas-id x y]]
                (-> (update-in db [::canvas canvas-id :basis :target] assoc :origin-x x :origin-y y)
                    (on-target-basis-change canvas-id))))

(reg-event-db ::auto-fit
              (fn [db [_ canvas-id]]
                (-> db
                    (update-in [::canvas canvas-id :basis :target] assoc :origin-x 0 :origin-y 0)
                    (on-target-basis-change canvas-id))))

(defn- init-size-target-basis
  [canvas-id]
  (when-let [el (.getElementById js/document (name canvas-id))]
    (let [rect (.getBoundingClientRect el)
          width (.-width rect)
          height (.-height rect)]
      (dispatch [::size-target-basis
                 canvas-id
                 {:width width
                  :height height}]))))

;;;; ********************************************************************************
;; Drag render element

(reg-sub ::drag-render-element-data
         (fn [db [_ canvas-id]] (get-in db [::canvas canvas-id :drag-render-element-data])))

(reg-event-db
 ::drag-render-element-start
 (fn [db [_ canvas-id drag-render-element-start]]
   (assoc-in db [::canvas canvas-id :drag-render-element-data] drag-render-element-start)))

(reg-event-db ::drag-render-element-end
              (fn [db [_ canvas-id]]
                (update-in db [::canvas canvas-id] dissoc :drag-render-element-data)))

(defn- on-drag-render-element-mouse-move
  [canvas-id drag-render-element-data translation]
  (let [{:keys [sd-x sd-y origin-x origin-y]} translation
        {:keys [parent render-id pos-in-x pos-in-y]} drag-render-element-data
        rect (.getBoundingClientRect parent)]
    (fn [e]
      (dispatch [::render-item-move
                 canvas-id
                 render-id
                 (/ (- (.-clientX e) (.-x rect) pos-in-x origin-x) sd-x)
                 (/ (- (.-clientY e) (.-y rect) pos-in-y origin-y) sd-y)]))))

(defn- on-drag-render-element-mouse-down
  [canvas-id drag-mode? render-id e]
  (when drag-mode?
    (let [p (.getElementById js/document (name canvas-id))
          target (.-target e)
          rect (.getBoundingClientRect target)
          client-x (.-clientX e)
          client-y (.-clientY e)
          pos-in-x (- client-x (.-x rect))
          pos-in-y (- client-y (.-y rect))]
      (dispatch [::drag-render-element-start
                 canvas-id
                 {:render-id render-id
                  :parent p
                  :pos-in-x pos-in-x
                  :client-x client-x
                  :client-y client-y
                  :pos-in-y pos-in-y}]))))

;; ********************************************************************************
;; Drag basis

(reg-sub ::drag-basis-data
         (fn [db [_ canvas-id]] (get-in db [::canvas canvas-id :drag-basis-data])))

(reg-event-db ::drag-basis-start
              (fn [db [_ canvas-id m]]
                (-> db
                    (assoc-in [::canvas canvas-id :drag-basis-data] m))))

(reg-event-db ::drag-basis-end
              (fn [db [_ canvas-id]]
                (-> db
                    (update-in [::canvas canvas-id] dissoc :drag-basis-data))))

(defn- on-drag-basis-mouse-move
  [canvas-id drag-basis-data]
  (fn [e]
    (let [{:keys [parent pos-in-x pos-in-y]} drag-basis-data
          rect (.getBoundingClientRect parent)]
      (dispatch [::move-target-basis
                 canvas-id
                 (- (.-clientX e) (.-x rect) pos-in-x)
                 (- (.-clientY e) (.-y rect) pos-in-y)]))))

(defn- on-drag-basis-mouse-down
  [canvas-id]
  (fn [e]
    (let [p (.getElementById js/document (name canvas-id))
          target (.-target e)
          rect (.getBoundingClientRect target)
          client-x (.-clientX e)
          client-y (.-clientY e)
          pos-in-x (- client-x (.-x rect))
          pos-in-y (- client-y (.-y rect))
          m {:parent p
             :pos-in-x pos-in-x
             :pos-in-y pos-in-y
             :client-x client-x
             :client-y client-y}]
      (dispatch [::drag-basis-start canvas-id m]))))

;; ********************************************************************************

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

(defn- update-src-basis
  [canva]
  (let [render-items (:render-items canva)
        res (reduce (fn [res render-item]
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
    (update-in canva [:basis :src] assoc :width (- max-x min-x) :height (- max-y min-y))))

;; ********************************************************************************
;; Public API

(defn render-elt-trajectory
  [t t-src re-src t-dst re-dst]
  (let [{x-src :x
         y-src :y}
        re-src
        {x-dst :x
         y-dst :y}
        re-dst
        index (/ (- t-dst t-src) (- t t-src))]
    (assoc t-src :x (* index (- x-dst x-src)) :y (* index (- y-dst y-src)))))

(reg-event-db ::set-all-canvas
              (fn [_db [_ canvas]]
                {::canvas (update-vals canvas
                                       (fn [canva]
                                         (-> canva
                                             (update :render-items
                                                     #(update-vals
                                                       %
                                                       (fn [render-item]
                                                         (update render-item :render-id keyword))))
                                             (update :mode keyword))))}))

(reg-event-db ::set-render-items
              (fn [db [_ canvas-id render-items]]
                (init-size-target-basis canvas-id)
                (-> db
                    (update ::canvas
                            assoc
                            canvas-id
                            (-> {:render-items render-items}
                                update-src-basis))
                    (on-target-basis-change canvas-id))))

(defn control-bar
  [canvas-id]
  (let [drag-render-element-data @(subscribe [::drag-render-element-data canvas-id])
        pressed {:box-shadow "0.3em 0.4em #888888"
                 :border-width "0.1em"}]
    [:div.bar
     [:button.w3-btn {:on-click #(dispatch [::save-to-ls])}
      [:i.fa.fa-save]]
     [:button.w3-btn {:on-click #(dispatch [::load-from-ls])}
      [:i.fa.fa-folder-open]]
     [:i "|"]
     [:button.w3-btn {:on-click #(dispatch [::auto-fit canvas-id])}
      [:i.fa.fa-expand]]
     (let [drag-mode? @(subscribe [::drag-mode? canvas-id])]
       [:button.w3-btn {:on-click #(do (dispatch [::toggle-drag-mode canvas-id])
                                       (when drag-render-element-data
                                         (dispatch [::drag-render-element-end canvas-id])))
                        :class (when drag-mode? "w3-border")
                        :style (when drag-mode? pressed)}
        [:i.fa.fa-arrows]])]))

(defn layout
  [opts canvas-id {:keys [multi-select-mode?]}]
  (r/create-class
   {:display-name "layout"
    :component-did-mount (fn [_]
                           (let [el (.getElementById js/document (name canvas-id))
                                 rect (.getBoundingClientRect el)
                                 width (.-width rect)
                                 height (.-height rect)]
                             (dispatch [::size-target-basis
                                        canvas-id
                                        {:width width
                                         :height height}])))
    :reagent-render
    (fn []
      (let [drag-render-element-data @(subscribe [::drag-render-element-data canvas-id])
            drag-basis-data @(subscribe [::drag-basis-data canvas-id])
            translation @(subscribe [::basis-translation canvas-id])
            drag-mode? @(subscribe [::drag-mode? canvas-id])
            render-items @(subscribe [::render-items canvas-id])]
        [:div
         (-> opts
             (update :style assoc
                     :position "relative"
                     :overflow "hidden"
                     :cursor (when drag-mode? "move"))
             (assoc
              :on-mouse-move
              (cond
                drag-render-element-data
                (on-drag-render-element-mouse-move canvas-id drag-render-element-data translation)
                drag-basis-data (on-drag-basis-mouse-move canvas-id drag-basis-data))
              :id canvas-id
              :on-click
              (fn [_] (when (one-selected? render-items) (dispatch [::clear-selection canvas-id])))
              :on-mouse-down (when drag-mode? (on-drag-basis-mouse-down canvas-id))
              :on-mouse-leave #(do (when drag-render-element-data
                                     (dispatch [::drag-render-element-end canvas-id]))
                                   (when drag-basis-data (dispatch [::drag-basis-end canvas-id])))
              :on-mouse-up #(do (when drag-render-element-data
                                  (dispatch [::drag-render-element-end canvas-id]))
                                (when drag-basis-data (dispatch [::drag-basis-end canvas-id])))))
         (mapcat (fn [{:keys [render-id]
                       :as render-item}]
                   [^{:key render-id}
                    [icon {:on-click #(do (.stopPropagation %)
                                          (dispatch [::select-render-element
                                                     canvas-id
                                                     render-id
                                                     (.-metaKey %)
                                                     multi-select-mode?]))
                           :on-mouse-down
                           #(do
                              (.stopPropagation %)
                              (on-drag-render-element-mouse-down canvas-id drag-mode? render-id %))}
                     render-item]])
          (vals render-items))]))}))
