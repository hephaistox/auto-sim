(ns auto-sim.devcards
  "Entry point for devcard app"
  (:require
   ["highlight.js" :as hljs]
   ["marked"       :as marked]
   [devcards.core  :as             dc
                   :include-macros true]
   [re-frame.core  :as re-frame]))

;; devcards uses global DevcardsSyntaxHighlighter and DevcardsMarked
;; ex. https://github.com/bhauman/devcards/blob/master/src/devcards/util/markdown.cljs#L28
;; therefore we need to define them
(js/goog.exportSymbol "DevcardsSyntaxHighlighter" hljs)
(js/goog.exportSymbol "DevcardsMarked" marked)

(defn add-css-link!
  [href]
  (let [link (.createElement js/document "link")]
    (set! (.-rel link) "stylesheet")
    (set! (.-type link) "text/css")
    (set! (.-href link) href)
    (.appendChild (.-head js/document) link)))

(defn ^:export init
  []
  (add-css-link! "/css/components.css")
  (add-css-link! "/css/w3_colors_flat.css")
  (add-css-link! "fontawesome/css/all.css")
  (add-css-link! "fontawesome/css/all.min.css")
  (add-css-link! "/css/w3_schools.css")
  (dc/start-devcard-ui!))
