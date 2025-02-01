(ns auto-sim.simulation-engine.response-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [automaton-core.adapters.schema                     :as core-schema]
   [auto-sim.simulation-engine          :as-alias sim-engine]
   [auto-sim.simulation-engine.response :as sut]))

(def event-stub
  #:auto-sim.simulation-engine{:type :a
                                              :date 1})

(deftest schema-test (is (= nil (core-schema/validate-humanize sut/schema)) "Test schema"))

(deftest build-test
  (is
   (=
    nil
    (->>
      #:auto-sim.simulation-engine{:stopping-causes []
                                                  :snapshot
                                                  #:auto-sim.simulation-engine{:id 1
                                                                                              :iteration
                                                                                              1
                                                                                              :date
                                                                                              1
                                                                                              :state
                                                                                              {}
                                                                                              :past-events
                                                                                              []
                                                                                              :future-events
                                                                                              [event-stub]}}
      (core-schema/validate-data-humanize (core-schema/close-map-schema sut/schema))))
   "Build response complies the schema."))

(deftest add-stopping-cause-test
  (is (= #:auto-sim.simulation-engine{:stopping-causes [{:a :b}]}
         (sut/add-stopping-cause {::sim-engine/stopping-causes []} {:a :b}))))

(defn- get-first-stopping-definition
  [response]
  (->> response
       ::sim-engine/stopping-causes
       first
       ::sim-engine/stopping-criteria
       ::sim-engine/stopping-definition
       ::sim-engine/id))

(defn- get-date
  [response]
  (->> response
       ::sim-engine/snapshot
       ::sim-engine/date))

(deftest consume-first-event-test
  (is
   (=
    #:auto-sim.simulation-engine{:id 4
                                                :iteration 4
                                                :date 10
                                                :state {}
                                                :past-events []
                                                :future-events []}
    (->
      #:auto-sim.simulation-engine{:stopping-causes []
                                                  :snapshot
                                                  #:auto-sim.simulation-engine{:id 3
                                                                                              :iteration
                                                                                              3
                                                                                              :date
                                                                                              10
                                                                                              :state
                                                                                              {}
                                                                                              :past-events
                                                                                              []
                                                                                              :future-events
                                                                                              []}}
      (sut/consume-first-event event-stub)
      ::sim-engine/snapshot))
   "No future events implies no modification of date, but the incrementation of iteration and id.")
  (is
   (=
    [::sim-engine/causality-broken 10]
    (->
      #:auto-sim.simulation-engine{:stopping-causes []
                                                  :snapshot
                                                  #:auto-sim.simulation-engine{:id 3
                                                                                              :iteration
                                                                                              3
                                                                                              :date
                                                                                              10
                                                                                              :state
                                                                                              {}
                                                                                              :past-events
                                                                                              []
                                                                                              :future-events
                                                                                              [event-stub]}}
      (sut/consume-first-event event-stub)
      ((juxt get-first-stopping-definition get-date))))
   "If the future event is happening in the past, then it breaks causality, the `date` is unchanged.")
  (is
   (empty?
    (->
      #:auto-sim.simulation-engine{:stopping-causes []
                                                  :snapshot
                                                  #:auto-sim.simulation-engine{:id 3
                                                                                              :iteration
                                                                                              3
                                                                                              :date
                                                                                              1
                                                                                              :state
                                                                                              {}
                                                                                              :past-events
                                                                                              []
                                                                                              :future-events
                                                                                              [event-stub]}}
      (sut/consume-first-event event-stub)
      ::sim-engine/stopping-causes))
   "A future event at the same date than the current snapshot is not creating causality issue.")
  (is
   (empty?
    (->
      #:auto-sim.simulation-engine{:stopping-causes []
                                                  :snapshot
                                                  #:auto-sim.simulation-engine{:id 3
                                                                                              :iteration
                                                                                              3
                                                                                              :date
                                                                                              0
                                                                                              :state
                                                                                              {}
                                                                                              :past-events
                                                                                              []
                                                                                              :future-events
                                                                                              [event-stub]}}
      (sut/consume-first-event event-stub)
      ::sim-engine/stopping-causes))
   "A future event later than the current snapshot is not creating causality issue."))

(deftest add-current-event-to-stopping-causes-test
  (is
   (=
    #:auto-sim.simulation-engine{:stopping-causes
                                                [#:auto-sim.simulation-engine{:a :b
                                                                                             :current-event
                                                                                             event-stub}
                                                 #:auto-sim.simulation-engine{:c :d
                                                                                             :current-event
                                                                                             event-stub}]
                                                :snapshot nil}
    (sut/add-current-event-to-stopping-causes
     #:auto-sim.simulation-engine{:stopping-causes
                                                 [#:auto-sim.simulation-engine{:a :b}
                                                  #:auto-sim.simulation-engine{:c
                                                                                              :d}]
                                                 :snapshot nil}
     event-stub))))
