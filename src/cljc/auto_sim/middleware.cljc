(ns auto-sim.middleware
  "Middlewares for simulaion handler"
  (:require
   [auto-sim.engine :as-alias sim-engine]))

(defn wrap-tap-request
  "Wrap an handler to tap the request"
  [handler]
  (fn [current-event state future-events]
    (tap> {:current-event current-event
           :state state
           :future-events future-events})
    (handler current-event state future-events)))

(defn wrap-tap-response
  "Wrap an handler to tap the response"
  [handler]
  (fn [current-event state future-events]
    (let [response (handler current-event state future-events)]
      (tap> {:current-event current-event
             :state state
             :response response
             :future-events future-events})
      response)))

(defn wrap-rendering
  "Wrap the `handler` to apply the rendering function `rendering-fn` to the state and then executes the handler."
  [rendering-fn handler]
  (fn [current-event state future-events]
    (let [res (handler current-event state future-events)]
      (rendering-fn (:state res))
      res)))

(defn wrap
  "Wrap the `handler` with the middlewares.

  Returns the handler value wrapped in all the middlewares."
  [middlewares handler]
  (reduce (fn [handler middleware] (middleware handler)) handler middlewares))
