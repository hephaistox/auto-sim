(ns auto-sim.ordering
  "Event ordering is used to have a reliable and repeatable sorting of events in the scheduler.

  An event ordering should be:
  * a total order
  * disambiguating simultaneous events

  ## Date ordering
  Date ordering is sorting events by date
  It could be done with `compare-field` on the date field of the event (typically `::sim-engine/date`)

  ## Simultaneous event ordering
  A simultaneous event ordering apply user defined decision on which event should be executed first
  It could leverage `compare-types` and `compare-field` or what user defined functions

  ## Simultaneous events
  Two or more events are simultaneous if they happen at the same date in the scheduler."
  (:require
   [auto-sim.engine :as-alias sim-engine]))

(defn fields
  "Returns a function to compare `e1` and `e2` based on values of field `field`. `nil` values are considered like `infinity`."
  [field]
  (fn [e1 e2]
    (let [d1 (field e1)
          d2 (field e2)]
      (cond
        (nil? d1) 666
        (nil? d2) -666
        :else (compare d1 d2)))))

(defn types
  "Compares two events `e1` and `e2` with their types, based on their ordering in `evt-type-priorities`, list of event priorities, ordered with higher priority first in the list.
  Returns the difference of their position in the `evt-type-priorities`, which is `0` in case of equality, negative if `e1` is before `e2`."
  [evt-type-priorities]
  (fn [e1 e2]
    (let [te1 (::sim-engine/type e1)
          te2 (::sim-engine/type e2)]
      (cond
        (nil? te1) 666
        (nil? te2) -666
        (not= te1 te2) (- (.indexOf evt-type-priorities te1) (.indexOf evt-type-priorities te2))
        :else 0))))

(defn hashes
  "Compares two events with their hash value"
  []
  (fn [e1 e2] (compare (hash e1) (hash e2))))

(defn- orders
  "Orders events by date."
  [order-fns e1 e2]
  (loop [[order-fn & rorder-fns] order-fns]
    (if (nil? order-fn) 0 (let [res (order-fn e1 e2)] (if (zero? res) (recur rorder-fns) res)))))

(defn sorter
  "A `sorter` returns a function to sort events. The comparison is done in the order of elements in `order-fns`.

  Returns a function with two events as parameters and returning the comparison of them, according to event-orderings."
  [& order-fns]
  (fn [events] (sort (fn [e1 e2] (orders order-fns e1 e2)) events)))
