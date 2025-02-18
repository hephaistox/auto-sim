(ns auto-sim.engine
  "The simulation engine is a `scheduler` working with a `model` describing the problem to solve.

  The `initial-snapshot` function creates the snapshot to start with.
  Then, `continue` allows to execute this `model`. It is starting with a snapshot that can be coming from an `initial-snapshot` or another `continue` execution.

  An event execution has three parameters `(event-execution current-event event-bucket state new-future-events)`:
  * `current-event` which is the current event to execute
  * `state` which is the state value before the event execution
  * `new-future-events` which is the list of future events without the current event

  The returned value is a map with `state` and `future-events` keys."
  (:require
   [auto-sim.engine :as-alias sim-engine]))

;; ********************************************************************************
;; Helpers
;; ********************************************************************************

(defn- continue*
  [model initial-snapshot]
  (let [{::sim-engine/keys [event-registry sorter]} model]
    (if-not (fn? sorter)
      (-> initial-snapshot
          (update ::sim-engine/stopping-criteria
                  conj
                  #::sim-engine{:stopping-criteria ::sim-engine/missing-sorter}))
      (let [{::sim-engine/keys [bucket future-events id iteration state past-events]}
            initial-snapshot]
        (loop [current-bucket bucket
               future-events (sorter future-events)
               iteration-offset 0
               state state
               past-events past-events]
          (if (empty? future-events)
            #::sim-engine{:bucket current-bucket
                          :future-events []
                          :id (+ id iteration-offset)
                          :iteration (+ iteration iteration-offset)
                          :state state
                          :past-events past-events
                          :stopping-criteria [#::sim-engine{:stopping-criteria
                                                            ::sim-engine/no-future-events}]}
            (let [[current-event & rfuture-events] future-events
                  {event-bucket ::sim-engine/bucket
                   event-type ::sim-engine/type}
                  current-event
                  event-execution (get event-registry event-type)]
              (cond
                (not (fn? event-execution))
                #::sim-engine{:bucket current-bucket
                              :future-events rfuture-events
                              :id (+ id iteration-offset)
                              :iteration (+ iteration iteration-offset)
                              :state state
                              :past-events (conj past-events current-event)
                              :stopping-criteria
                              [#::sim-engine{:stopping-criteria ::sim-engine/execution-not-found
                                             :possible-types (vec (keys event-registry))
                                             :not-found-type (::sim-engine/type current-event)}]}
                (< event-bucket current-bucket)
                #::sim-engine{:bucket current-bucket
                              :future-events rfuture-events
                              :id (+ id iteration-offset)
                              :iteration (+ iteration iteration-offset)
                              :state state
                              :past-events (conj past-events current-event)
                              :stopping-criteria [#::sim-engine{:stopping-criteria
                                                                ::sim-engine/causality-broken
                                                                :current-bucket current-bucket
                                                                :event-bucket event-bucket}]}
                :else (let [iteration-offset (inc iteration-offset)
                            event-return
                            (try (event-execution {::state state
                                                   ::future-events rfuture-events}
                                                  current-event
                                                  event-bucket)
                                 (catch #?(:clj Exception
                                           :cljs :default)
                                   e
                                   #::sim-engine{:bucket event-bucket
                                                 :future-events rfuture-events
                                                 :id (+ id iteration-offset)
                                                 :iteration (+ iteration iteration-offset)
                                                 :state state
                                                 :past-events (conj past-events current-event)
                                                 :stopping-criteria
                                                 [#::sim-engine{:stopping-criteria
                                                                ::sim-engine/failed-event-execution
                                                                :current-event current-event
                                                                :exception e}]}))
                            {::sim-engine/keys [future-events state stopping-criteria errors]}
                            event-return]
                        (cond
                          stopping-criteria event-return
                          errors #::sim-engine{:bucket event-bucket
                                               :future-events future-events
                                               :id (+ id iteration-offset)
                                               :iteration (+ iteration iteration-offset)
                                               :state state
                                               :past-events (conj past-events current-event)
                                               :stopping-criteria
                                               [#::sim-engine{:stopping-criteria
                                                              ::sim-engine/error-happen
                                                              :current-event current-event
                                                              :errors errors}]}
                          :else (recur event-bucket
                                       (sorter future-events)
                                       iteration-offset
                                       state
                                       (conj past-events current-event))))))))))))

;; ********************************************************************************
;; API
;; ********************************************************************************

(defn initial-snapshot
  "Returns the initial snapshot for a scheduler.

  It is initialiazed with `future-events` (will be turned into a vec to maintain proper order) and `state`.

  Starts at `iteration` and `id` `1`, `bucket` 0, with no `past-events`."
  [bucket state future-events]
  #::sim-engine{:bucket bucket
                :future-events (vec future-events)
                :id 1
                :iteration 1
                :state (if (empty? state) {} state)
                :past-events []})

(defn continue
  "Continue execution of model, starts with `snapshot`.

  The scheduler executes the `model` until a `stopping-criteria` is met.

  Model should contain:
  * `#::sim-engine/sorter` the result of an execution of function `auto-sim.ordering/sorter`
  * `#::sim-engine/event-registry` a map associating the `::sim-engine/type` to a function: `(f current-event state future-events)`

  Note that users can enrich its execution by:
  * enriching the `model` with augmented registries.
  * supplementary middlewares and stopping criteria that don't affect the model

  Note that particular attention has been paid to leverage model's preparation, e.g. stopping-criteria and middlewares aren't translated again, just their `scheduler` version is.

  Returns last snapshot and the `stopping-criteria`."
  [model initial-snapshot]
  (-> (continue* model initial-snapshot)
      (update ::sim-engine/future-events vec)
      (update ::sim-engine/past-events vec)
      (update ::sim-engine/stopping-criteria vec)))
