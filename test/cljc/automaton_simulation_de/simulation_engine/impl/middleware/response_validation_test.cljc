(ns auto-sim.simulation-engine.impl.middleware.response-validation-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.simulation-engine                                     :as-alias sim-engine]
   [auto-sim.simulation-engine.impl.middleware.response-validation :as sut]
   [auto-sim.simulation-engine.impl.stopping.cause                 :as sim-de-stopping-cause]
   [automaton-core.adapters.schema                                 :as core-schema]))

(def event-stub
  #:auto-sim.simulation-engine{:type :a
                               :date 1})

(deftest evaluates-test
  (is
   (=
    nil
    (->
      #:auto-sim.simulation-engine{:stopping-causes []
                                   :snapshot
                                   #:auto-sim.simulation-engine{:id 1
                                                                :iteration 1
                                                                :date 1
                                                                :state {}
                                                                :past-events []
                                                                :future-events
                                                                [event-stub
                                                                 #:auto-sim.simulation-engine{:type
                                                                                              :b
                                                                                              :date
                                                                                              2}]}}
      (sut/evaluates event-stub)))
   "Well form response returns `nil`.")
  (is (= nil
         (core-schema/validate-data-humanize sim-de-stopping-cause/schema
                                             (sut/evaluates nil event-stub)))
      "When detecting an issue, evaluates returns a map complying to `stopping-cause schema`."))

(deftest wrap-response-test
  (is
   (=
    []
    (->>
      nil
      ((sut/wrap-response
        (fn [_request]
          (->>
            #:auto-sim.simulation-engine{:stopping-causes []
                                         :snapshot
                                         #:auto-sim.simulation-engine{:id 1
                                                                      :iteration 1
                                                                      :date 1
                                                                      :state {}
                                                                      :past-events []
                                                                      :future-events
                                                                      [event-stub
                                                                       #:auto-sim.simulation-engine{:type
                                                                                                    :b
                                                                                                    :date
                                                                                                    2}]}}))))
      ::sim-engine/stopping-causes))
   "Non of response.")
  (is (seq (->> nil
                ((sut/wrap-response (fn [_request] {:foo true})))
                ::sim-engine/stopping-causes))
      "Invalid response is detected."))
