(ns auto-sim.charts
  (:require
   [reagent.core :as reagent]))

(defn- canva-opts
  [canva-id label]
  {:class "w3-border.w3-row"
   :id canva-id
   :role "img"
   :aria-label label})

(defn init-barchart
  [canva-id data]
  (fn [_] (let [canvas (.getElementById js/document canva-id)] (js/Chart. canvas (clj->js data)))))

(defn bar-chart
  [data]
  (let [canva-id (random-uuid)]
    (reagent/create-class {:display-name "barchart"
                           :component-did-mount (init-barchart canva-id data)
                           :reagent-render (fn [_] [:canvas (canva-opts canva-id "Bar chart")])})))

(defn init-bubblechart
  [canva-id data]
  (fn [_] (let [canvas (.getElementById js/document canva-id)] (js/Chart. canvas (clj->js data)))))

(defn bubble-chart
  [data]
  (let [canva-id (random-uuid)]
    (reagent/create-class {:display-name "barchart"
                           :component-did-mount (init-bubblechart canva-id data)
                           :reagent-render (fn [_] [:canvas
                                                    (canva-opts canva-id "Bubble chart")])})))

(defn init-doughnut-chart
  [canva-id data]
  (fn [_] (let [canvas (.getElementById js/document canva-id)] (js/Chart. canvas (clj->js data)))))

(defn doughnut-chart
  [data]
  (let [canva-id (random-uuid)]
    (reagent/create-class {:display-name "doughnut"
                           :component-did-mount (init-doughnut-chart canva-id data)
                           :reagent-render (fn [_] [:canvas
                                                    (canva-opts canva-id "Doughtnut chart")])})))

(defn init-line-chart
  [canva-id data]
  (fn [_] (let [canvas (.getElementById js/document canva-id)] (js/Chart. canvas (clj->js data)))))

(defn line-chart
  [data]
  (let [canva-id (random-uuid)]
    (reagent/create-class {:display-name "linechart"
                           :component-did-mount (init-line-chart canva-id data)
                           :reagent-render (fn [_] [:canvas (canva-opts canva-id "Line chart")])})))

(defn init-polar-chart
  [canva-id data]
  (fn [_] (let [canvas (.getElementById js/document canva-id)] (js/Chart. canvas (clj->js data)))))

(defn polar-chart
  [data]
  (let [canva-id (random-uuid)]
    (reagent/create-class {:display-name "polarchart"
                           :component-did-mount (init-polar-chart canva-id data)
                           :reagent-render (fn [_] [:canvas
                                                    (canva-opts canva-id "Polar chart")])})))
