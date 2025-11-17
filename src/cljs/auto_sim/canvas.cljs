(ns auto-sim.canvas
  "Canvas is drawing graphically `render-items`, a map associating a `render-item` to a `name`

  A `render-item` is made of:
  - `render-id`
  - `img` an uri to the image to display
  - `render-items-basis` a map describing:
     - `width` and `height`
     - `x` and `y`

  When displayed in a canvas, each `render-item` will be completed with `canvas-basis` to describe where that item will be displayed in the canvas"
  (:require
   [re-frame.core :refer [dispatch reg-event-db reg-sub subscribe]]
   [reagent.core  :as r]))

;; ********************************************************************************
;; Render-element selection

(reg-event-db
 ::select-render-element
 (fn [db [_ canvas-id render-id multi-select-mode?]]
   (let [multi-select? multi-select-mode?]
     (cond-> db
       (not multi-select?)
       (update-in [::canvas canvas-id :render-items] update-vals #(dissoc % :item-selected?))
       :else (update-in [::canvas canvas-id :render-items render-id :item-selected?] not)))))

(defn- one-selected?
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
;; Basis translations

(reg-sub ::canvas-basis (fn [db [_ canvas-id]] (get-in db [::canvas canvas-id :canvas-basis])))

(reg-sub ::basis-translation
         (fn [db [_ canvas-id]] (get-in db [::canvas canvas-id :basis-translation])))

(reg-sub ::render-items-basis
         (fn [db [_ canvas-id]] (get-in db [::canvas canvas-id :render-items-basis])))

(defn- render-items-to-canvas
  "Returns the parameters of the affine function to translate from `render-items` to `canvas` basis.

  If one parameter is missing, returns `nil`"
  [render-items-basis canvas-basis]
  (let [{render-items-width :width
         render-items-height :height}
        render-items-basis
        {origin-x-target :origin-x
         origin-y-target :origin-y
         canvas-width :width
         canvas-height :height}
        canvas-basis]
    (when (every? some? [canvas-height canvas-width render-items-width render-items-height])
      {:origin-x origin-x-target
       :origin-y origin-y-target
       :sd-x (/ canvas-width render-items-width)
       :sd-y (/ canvas-height render-items-height)})))

;; ********************************************************************************
;; render-item

(defn- calculate-canvas-basis
  "Add its `canvas` basis to a `render-item`."
  [render-item translation]
  (let [{:keys [render-items-basis]} render-item
        {:keys [origin-x origin-y sd-x sd-y]} translation
        {:keys [width height x y]} render-items-basis]
    (assoc render-item
           :canvas-basis
           {:width (js/Math.round (* sd-x width))
            :height (js/Math.round (* sd-y height))
            :x (js/Math.round (+ origin-x (* sd-x x)))
            :y (js/Math.round (+ origin-y (* sd-y y)))})))

;; ********************************************************************************
;; Canvas

(reg-event-db ::canvas-resized
              (fn [db [_ canvas-id width height]]
                (let [render-items-basis (get-in db [::canvas canvas-id :render-items-basis])
                      canvas-basis {:origin-x 0
                                    :origin-y 0
                                    :width width
                                    :height height}
                      basis-translation (render-items-to-canvas render-items-basis canvas-basis)]
                  (-> db
                      (update-in [::canvas canvas-id]
                                 (fn [canva]
                                   (cond-> canva
                                     true (assoc :basis-translation basis-translation
                                                 :canvas-basis canvas-basis)
                                     (:render-items canva)
                                     (update :render-items
                                             update-vals
                                             #(calculate-canvas-basis % basis-translation)))))))))

;; ********************************************************************************
;; Dragging mode

(reg-event-db
 ::drag-render-element-start
 (fn [db [_ canvas-id drag-render-element-start]]
   (assoc-in db [::canvas canvas-id :drag-render-element-data] drag-render-element-start)))

(reg-event-db ::drag-render-element-end
              (fn [db [_ canvas-id]]
                (update-in db [::canvas canvas-id] dissoc :drag-render-element-data)))

;; New event for updating render element position during drag
(reg-event-db
 ::drag-render-element-move
 (fn [db [_ canvas-id client-x client-y]]
   (let [drag-data (get-in db [::canvas canvas-id :drag-render-element-data])]
     (if drag-data
       (let [{:keys [render-id pos-in-x pos-in-y]} drag-data
             basis-translation (get-in db [::canvas canvas-id :basis-translation])
             {:keys [sd-x sd-y origin-x origin-y]} basis-translation
             canvas-element (.getElementById js/document (name canvas-id))
             canvas-rect (.getBoundingClientRect canvas-element)
             ;; Calculate mouse position relative to canvas
             canvas-mouse-x (- client-x (.-left canvas-rect))
             canvas-mouse-y (- client-y (.-top canvas-rect))
             ;; Calculate new top-left position of the item (accounting for the offset within the item)
             new-canvas-x (- canvas-mouse-x pos-in-x)
             new-canvas-y (- canvas-mouse-y pos-in-y)
             ;; Convert back to render-items coordinates
             new-render-x (/ (- new-canvas-x origin-x) sd-x)
             new-render-y (/ (- new-canvas-y origin-y) sd-y)]
         (-> db
             ;; Update render-items-basis position
             (assoc-in [::canvas canvas-id :render-items render-id :render-items-basis :x]
                       new-render-x)
             (assoc-in [::canvas canvas-id :render-items render-id :render-items-basis :y]
                       new-render-y)
             ;; Update canvas-basis position
             (assoc-in [::canvas canvas-id :render-items render-id :canvas-basis :x] new-canvas-x)
             (assoc-in [::canvas canvas-id :render-items render-id :canvas-basis :y] new-canvas-y)))
       db))))

;; Drag basis events
(reg-event-db ::drag-basis-start
              (fn [db [_ canvas-id drag-basis-data]]
                (assoc-in db [::canvas canvas-id :drag-basis-data] drag-basis-data)))

(reg-event-db ::drag-basis-end
              (fn [db [_ canvas-id]]
                (let [render-items (get-in db [::canvas canvas-id :render-items])
                      new-render-items-basis (render-items-basis render-items)]
                  (-> db
                      (update-in [::canvas canvas-id] dissoc :drag-basis-data)
                      (assoc-in [::canvas canvas-id :render-items-basis] new-render-items-basis)))))

(reg-event-db
 ::drag-basis-move
 (fn [db [_ canvas-id client-x client-y]]
   (let [drag-data (get-in db [::canvas canvas-id :drag-basis-data])]
     (if drag-data
       (let [{:keys [client-x client-y]} drag-data
             canvas-element (.getElementById js/document (name canvas-id))
             canvas-rect (.getBoundingClientRect canvas-element)
             ;; Calculate mouse position relative to canvas
             canvas-mouse-x (- client-x (.-left canvas-rect))
             canvas-mouse-y (- client-y (.-top canvas-rect))
             prev-canvas-mouse-x (- (:client-x drag-data) (.-left canvas-rect))
             prev-canvas-mouse-y (- (:client-y drag-data) (.-top canvas-rect))
             ;; Calculate the delta movement
             dx (- canvas-mouse-x prev-canvas-mouse-x)
             dy (- canvas-mouse-y prev-canvas-mouse-y)
             basis-translation (get-in db [::canvas canvas-id :basis-translation])
             {:keys [sd-x sd-y]} basis-translation
             ;; Convert canvas delta to render-items delta
             render-dx (/ dx sd-x)
             render-dy (/ dy sd-y)]
         (-> db
             ;; Update drag data with new client position
             (assoc-in [::canvas canvas-id :drag-basis-data :client-x] client-x)
             (assoc-in [::canvas canvas-id :drag-basis-data :client-y] client-y)
             ;; Move all render items by the delta
             (update-in [::canvas canvas-id :render-items]
                        update-vals
                        (fn [render-item]
                          (let [current-x (get-in render-item [:render-items-basis :x] 0)
                                current-y (get-in render-item [:render-items-basis :y] 0)
                                new-x (+ current-x render-dx)
                                new-y (+ current-y render-dy)
                                updated-render-item (-> render-item
                                                        (assoc-in [:render-items-basis :x] new-x)
                                                        (assoc-in [:render-items-basis :y] new-y))]
                            ;; Recalculate canvas position
                            (calculate-canvas-basis updated-render-item basis-translation))))))
       db))))

(reg-sub ::drag-render-element-data
         (fn [db [_ canvas-id]] (get-in db [::canvas canvas-id :drag-render-element-data])))

(reg-sub ::drag-basis-data
         (fn [db [_ canvas-id]] (get-in db [::canvas canvas-id :drag-basis-data])))

(reg-sub ::drag-mode? (fn [db [_ canvas-id]] (= :dragging (get-in db [::canvas canvas-id :mode]))))

(reg-event-db ::toggle-drag-mode
              (fn [db [_ canvas-id]]
                (if (= :dragging (get-in db [::canvas canvas-id :mode]))
                  (update-in db [::canvas canvas-id] dissoc :mode)
                  (assoc-in db [::canvas canvas-id :mode] :dragging))))

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

;; Auto-fit functionality
(reg-event-db ::auto-fit
              (fn [db [_ canvas-id]]
                (let [render-items (get-in db [::canvas canvas-id :render-items])
                      new-render-items-basis (render-items-basis render-items)
                      canvas-basis (get-in db [::canvas canvas-id :canvas-basis])
                      new-canvas-basis (assoc canvas-basis :origin-x 0 :origin-y 0)
                      new-basis-translation (render-items-to-canvas new-render-items-basis
                                                                    new-canvas-basis)]
                  (-> db
                      (assoc-in [::canvas canvas-id :render-items-basis] new-render-items-basis)
                      (assoc-in [::canvas canvas-id :canvas-basis] new-canvas-basis)
                      (assoc-in [::canvas canvas-id :basis-translation] new-basis-translation)
                      (update-in [::canvas canvas-id :render-items]
                                 update-vals
                                 #(calculate-canvas-basis % new-basis-translation))))))

;; ********************************************************************************
;; Render items

(reg-sub ::render-item-ids
         (fn [db [_ canvas-id]] (keys (get-in db [::canvas canvas-id :render-items]))))

(reg-sub ::render-item
         (fn [db [_ canvas-id render-item-id]]
           (get-in db [::canvas canvas-id :render-items render-item-id])))

(defn- render-item-bounds
  [render-item]
  (let [{:keys [x y width height]
         :or {x 0
              y 0}}
        (:render-items-basis render-item)]
    {:min-x x
     :max-x (+ x width)
     :min-y y
     :max-y (+ y height)}))

(defn- render-items-basis
  "Calculate the bounds of `render-items`, considering each of it as a box starting at `x` and `y`, with `width` and `height` size."
  [render-items]
  (let [render-items (map render-item-bounds (vals render-items))
        min-x (reduce min (map :min-x render-items))
        max-x (reduce max (map :max-x render-items))
        min-y (reduce min (map :min-y render-items))
        max-y (reduce max (map :max-y render-items))]
    {:min-x min-x
     :max-x max-x
     :width (- max-x min-x)
     :height (- max-y min-y)
     :min-y min-y
     :max-y max-y}))

;; ********************************************************************************
;; Control bar

(defn bn-autofit
  [canvas-id]
  [:button.w3-button {:on-click #(dispatch [::auto-fit canvas-id])}
   [:i.fa.fa-expand]])

(defn bn-drag-move
  [canvas-id]
  (let [pressed-opts {:box-shadow "0.3em 0.4em #888888"
                      :border-width "0.1em"}
        drag-mode? (subscribe [::drag-mode? canvas-id])]
    [:button.w3-button {:on-click #(do (dispatch [::toggle-drag-mode canvas-id])
                                       (dispatch [::drag-render-element-end canvas-id]))
                        :class (when @drag-mode? "w3-border")
                        :style (when @drag-mode? pressed-opts)}
     [:i.fa.fa-arrows]]))

;; ********************************************************************************
;; Layout

(defn- icon
  [opts render-item]
  (let [{:keys [img canvas-basis item-selected?]} render-item
        {:keys [x y width height]} canvas-basis]
    [:img.w3-hover-shadow
     (-> opts
         (assoc :draggable false :src img)
         (update
          :style
          #(cond->
             (assoc % :height height :width width :border (when item-selected? "0.2em solid black"))
             (or x y) (assoc :position "absolute" :top y :left x))))]))

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

;; Mouse move handler for the canvas
(defn- on-mouse-move
  [canvas-id e]
  (let [client-x (.-clientX e)
        client-y (.-clientY e)]
    (dispatch [::drag-render-element-move canvas-id client-x client-y])
    (dispatch [::drag-basis-move canvas-id client-x client-y])))

(defn layout*
  [opts canvas-id pars]
  (let [{:keys [multi-select-mode?]} pars]
    (r/create-class
     {:display-name "Simulation layout"
      :reagent-render
      (fn [_ _ _]
        (let [drag-mode? (subscribe [::drag-mode? canvas-id])
              drag-render-element-data @(subscribe [::drag-render-element-data canvas-id])
              drag-basis-data @(subscribe [::drag-basis-data canvas-id])
              render-item-ids (subscribe [::render-item-ids canvas-id])
              basis-translation (subscribe [::basis-translation canvas-id])
              canvas-basis (subscribe [::canvas-basis canvas-id])]
          [:div
           (-> opts
               (assoc
                :id canvas-id
                :on-click (fn [_] (dispatch [::clear-selection canvas-id]))
                :on-mouse-down (when @drag-mode? (on-drag-basis-mouse-down canvas-id))
                :on-mouse-move #(on-mouse-move canvas-id %)
                :on-mouse-leave #(do (when drag-render-element-data
                                       (dispatch [::drag-render-element-end canvas-id]))
                                     (when drag-basis-data (dispatch [::drag-basis-end canvas-id])))
                :on-mouse-up #(do (when drag-render-element-data
                                    (dispatch [::drag-render-element-end canvas-id]))
                                  (when drag-basis-data (dispatch [::drag-basis-end canvas-id]))))
               (update :style assoc
                       :position "relative"
                       :overflow "hidden"
                       :cursor (cond
                                 (and @drag-mode? drag-basis-data) "move"
                                 (and @drag-mode? drag-render-element-data) "grabbing"
                                 @drag-mode? "grab"
                                 :else "default")))
           ;; Center marker
           [:div.w3-border {:style {:left (/ (:width @canvas-basis) 2)
                                    :top (/ (:height @canvas-basis) 2)
                                    :position "absolute"
                                    :width "20px"
                                    :height "30px"
                                    :pointer-events "none"}}]
           ;; Render items
           [:div
            (->> @render-item-ids
                 (mapcat (fn [render-item-id]
                           (let [render-item (subscribe [::render-item canvas-id render-item-id])]
                             [^{:key render-item-id}
                              [icon {:on-click #(do (.stopPropagation %)
                                                    (dispatch [::select-render-element
                                                               canvas-id
                                                               render-item-id
                                                               multi-select-mode?]))
                                     :on-mouse-down #(do (.stopPropagation %)
                                                         (on-drag-render-element-mouse-down
                                                          canvas-id
                                                          @drag-mode?
                                                          render-item-id
                                                          %))}
                               @render-item]]))))]]))
      :component-did-mount (fn [_]
                             ;;NOTE We must wait the component is mounted to catch its dimensions
                             (let [el (.getElementById js/document (name canvas-id))
                                   rect (.getBoundingClientRect el)
                                   width (.-width rect)
                                   height (.-height rect)]
                               (dispatch [::canvas-resized canvas-id width height])))})))

;; ********************************************************************************
;; Public api

(reg-event-db ::set-render-items
              (fn [db [_ canvas-id render-items]]
                (let [render-items-basis (render-items-basis render-items)
                      canvas-basis (get-in db [::canvas canvas-id :canvas-basis])
                      basis-translation (render-items-to-canvas render-items-basis canvas-basis)]
                  (update-in db
                             [::canvas canvas-id]
                             (fn [canvas]
                               (assoc canvas
                                      :render-items
                                      (if basis-translation
                                        (update-vals render-items
                                                     #(calculate-canvas-basis % basis-translation))
                                        render-items)
                                      :render-items-basis render-items-basis
                                      :basis-translation basis-translation))))))

(defn layout
  [opts canvas-id pars]
  (if (map? opts) (layout* opts canvas-id pars) (layout* {} opts canvas-id)))
