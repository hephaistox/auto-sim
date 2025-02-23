(ns auto-sim.engine
  "The simulation engine is a `scheduler` working with a `model` describing the problem to solve.

  The `initial-snapshot` function updates the model with a snapshot to start with.
  Then, `continue` allows to execute this `model`. It is starting with a snapshot that can be coming from an `initial-snapshot` or another `continue` execution.

  An event execution has three parameters `(event-execution current-event event-bucket state new-future-events)`:
  * `current-event` which is the current event to execute
  * `state` which is the state value before the event execution
  * `new-future-events` which is the list of future events without the current event

  The returned value is a map with `state` and `future-events` keys."
  (:require
   [auto-sim.engine            :as-alias sim-engine]
   [auto-sim.stopping-criteria :as sim-sc]))

;; ********************************************************************************
;; Helpers
;; ********************************************************************************

(defn run-snapshot
  [model sorter stopping-definition event-registry]
  (let [{::sim-engine/keys [bucket future-events id iteration state past-events]} model]
    (loop [future-events (sorter future-events)
           bucket bucket
           iteration-offset 0
           state state
           past-events past-events]
      (let [[current-event & rfuture-events] future-events
            on-previous-model (fn [stopping-criteria]
                                (->
                                  model
                                  (assoc ::sim-engine/bucket bucket
                                         ::sim-engine/state state
                                         ::sim-engine/iteration (+ iteration-offset iteration)
                                         ::sim-engine/id (+ id iteration-offset)
                                         ::sim-engine/past-events past-events
                                         ::sim-engine/future-events future-events)
                                  (update ::sim-engine/stopping-criteria concat stopping-criteria)))
            on-next-event (fn [stopping-criteria]
                            (assoc (on-previous-model stopping-criteria)
                                   ::sim-engine/past-events (conj past-events current-event)
                                   ::sim-engine/future-events rfuture-events))
            stopping-criteria (and stopping-definition
                                   (sim-sc/eval stopping-definition (on-previous-model nil)))]
        (cond
          (empty? future-events) (-> [#::sim-engine{:id ::sim-engine/no-future-events
                                                    :doc ["No more future events to execute"]}]
                                     on-previous-model)
          (seq stopping-criteria) (on-previous-model stopping-criteria)
          :else
          (let [{event-bucket ::sim-engine/bucket
                 event-type ::sim-engine/type}
                current-event
                event-execution (get event-registry event-type)]
            (cond
              (not (fn? event-execution))
              (on-next-event
               [#::sim-engine{:id ::sim-engine/execution-not-found
                              :doc
                              ["Event has an unknown event type `%s` (event is `%s`, possible values are `%s`)"
                               event-type
                               current-event
                               (vec (keys event-registry))]
                              :event current-event
                              :possible-types (vec (keys event-registry))
                              :not-found-type (::sim-engine/type current-event)}])
              (< event-bucket bucket)
              (on-next-event
               [#::sim-engine{:id ::sim-engine/causality-broken
                              :doc
                              ["Internal error - causality-broken: bucket is %d, next-event bucket is %d (event is `%s`)"
                               bucket
                               event-bucket
                               current-event]
                              :current-bucket bucket
                              :event-bucket event-bucket}])
              :else
              (let [iteration-offset (inc iteration-offset)
                    event-return
                    (try (event-execution {::state state
                                           ::future-events rfuture-events}
                                          current-event
                                          event-bucket)
                         (catch #?(:clj Exception
                                   :cljs :default)
                           e
                           (on-next-event
                            [#::sim-engine{:id ::sim-engine/failed-event-execution
                                           :doc ["Internal error - failed execution of event"]
                                           :current-event current-event
                                           :exception e}])))
                    {new-future-events ::sim-engine/future-events
                     new-state ::sim-engine/state
                     new-stopping-criteria ::sim-engine/stopping-criteria
                     new-errors ::sim-engine/errors}
                    event-return
                    new-model-fn (fn [errors]
                                   (-> (on-next-event (cond-> new-stopping-criteria
                                                        errors (concat errors)))
                                       (assoc ::sim-engine/state new-state
                                              ::sim-engine/iteration (+ iteration-offset iteration)
                                              ::sim-engine/id (+ id iteration-offset)
                                              ::sim-engine/future-events new-future-events)))]
                (cond
                  new-stopping-criteria (new-model-fn nil)
                  new-errors
                  (-> [#::sim-engine{:id ::sim-engine/error-happens
                                     :doc ["Errors has been documented during event execution: %s"
                                           new-errors]
                                     :current-event current-event
                                     :errors new-errors}]
                      new-model-fn)
                  :else (recur (sorter new-future-events)
                               event-bucket
                               iteration-offset
                               new-state
                               (conj past-events current-event)))))))))))

(defn- continue*
  [model]
  (let [{::sim-engine/keys [event-registry sorter stopping-definition]} model
        model (-> model
                  (update ::sim-engine/iteration (fnil identity 0))
                  (update ::sim-engine/id (fnil identity 0)))]
    (cond
      (not (fn? sorter)) (-> model
                             (update ::sim-engine/stopping-criteria
                                     conj
                                     #::sim-engine{:id ::sim-engine/missing-sorter
                                                   :doc ["Event sorter is not defined"]}))
      :else (run-snapshot model sorter stopping-definition event-registry))))

;; ********************************************************************************
;; API
;; ********************************************************************************

(defn initial-snapshot
  "Returns the initial snapshot for a scheduler.

  It is initialiazed with `future-events` (will be turned into a vec to maintain proper order) and `state`.

  Starts at `iteration` and `id` `1`, `bucket` 0, with no `past-events`."
  [model bucket state future-events]
  (assoc model
         ::sim-engine/bucket bucket
         ::sim-engine/future-events (vec future-events)
         ::sim-engine/id 1
         ::sim-engine/iteration 1
         ::sim-engine/state (if (empty? state) {} state)
         ::sim-engine/past-events []))

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
  [model]
  (-> model
      continue*
      (update ::sim-engine/future-events vec)
      (update ::sim-engine/past-events vec)
      (update ::sim-engine/stopping-criteria vec)))

(defn reinit-sc
  [model]
  (assoc model ::sim-engine/stopping-definition (::sim-engine/cust-stopping-definition model)))

(defn run [model] (continue model))

(defn run-to
  "Run until time bucket `bucket`"
  [model bucket]
  (-> model
      reinit-sc
      (sim-sc/stop-bucket bucket)
      continue))

(defn run-iteration
  "Run until iteration `iteration`"
  [model iteration]
  (-> model
      reinit-sc
      (sim-sc/stop-iteration iteration)
      continue
      (update ::sim-engine/stopping-criteria
              (partial remove #(= ::sim-sc/iteration-stopping (::sim-engine/id %))))))

(defn clean-stop-criteria [model] (dissoc model ::sim-engine/stopping-criteria))
