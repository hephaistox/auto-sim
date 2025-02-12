(ns auto-sim.middleware-test
  (:require
   [auto-sim.engine     :as-alias sim-engine]
   [auto-sim.middleware :as sut]
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])))

(deftest wrap-tap-request-test
  (is (= {:state #:state{:foo :bar}
          :current-event {:current :event}
          :response true
          :future-events [{:foo :bar}]}
         ((sut/wrap-tap-request (fn [current-event state future-events]
                                  {:state state
                                   :current-event current-event
                                   :response true
                                   :future-events future-events}))
          {:current :event}
          #:state{:foo :bar}
          [{:foo :bar}]))
      "Wrap-tap-request is returning the handler executed, returned value shows handler execution"))

(deftest wrap-tap-response-test
  (is (= {:state #:state{:foo :bar}
          :current-event {:current :event}
          :response true
          :future-events [{:foo :bar}]}
         ((sut/wrap-tap-response (fn [current-event state future-events]
                                   {:state state
                                    :current-event current-event
                                    :response true
                                    :future-events future-events}))
          {:current :event}
          #:state{:foo :bar}
          [{:foo :bar}])))
  "Wrap-tap-response is returning the handler executed, returned value shows handler execution")

(deftest wrap-rendering-test
  (is (= "{:foo :bar}\n"
         (with-out-str ((sut/wrap-rendering (fn [state] (println state))
                                            (fn [current-event state future-events]
                                              {:state state
                                               :current-event current-event
                                               :response true
                                               :future-events future-events}))
                        {:current :event}
                        {:foo :bar}
                        [{:foo :bar}])))
      "Wrap-rendering is using the `rendering-fn` to print the state."))

(deftest wrap-test
  (is (= "state is: {:to-be :printed}\n"
         (with-out-str ((sut/wrap [(partial sut/wrap-rendering
                                            (fn [state] (println "state is:" state)))]
                                  (fn [current-event state future-events]
                                    {:state state
                                     :current-event current-event
                                     :response true
                                     :future-events future-events}))
                        {:current :event}
                        {:to-be :printed}
                        [{:foo :bar}])))
      "wrap is applying the wrap-rendering")
  (is (= "state1 is: {:to-be :printed}\nstate2 is: {:to-be :printed}\n"
         (with-out-str ((sut/wrap
                         [(partial sut/wrap-rendering (fn [state] (println "state1 is:" state)))
                          (partial sut/wrap-rendering (fn [state] (println "state2 is:" state)))]
                         (fn [current-event state future-events]
                           {:state state
                            :current-event current-event
                            :response true
                            :future-events future-events}))
                        {:current :event}
                        {:to-be :printed}
                        [{:foo :bar}])))
      "Two middlewares are executed"))
