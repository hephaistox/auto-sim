(ns auto-sim.devcards
  "Entry point for devcard app"
  (:require
   ["highlight.js" :as hljs]
   ["marked"       :as marked]
   [devcards.core  :as             dc
                   :include-macros true]))

(js/goog.exportSymbol "DevcardsSyntaxHighlighter" hljs)
(js/goog.exportSymbol "DevcardsMarked" marked)

(defn ^:export init [] (dc/start-devcard-ui!))
