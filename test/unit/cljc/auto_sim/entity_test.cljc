(ns auto-sim.entity-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.engine :as-alias sim-engine]
   [auto-sim.entity :as sut]))

(deftest create-test
  (is (= #::sim-engine{:entity {:uuid-1 #::sim-engine{:created 3
                                                      :living 3
                                                      :entity-state {:foo :bar}}}}
         (sut/create {} 3 :foo-entity :uuid-1 {:foo :bar}))
      "Adding a new entity that was not existing before.")
  (is
   (= {::sim-engine/entity {:uuid-1 #::sim-engine{:created 4
                                                  :entity-state {:foo :c
                                                                 :a :b}
                                                  :errors
                                                  [#::sim-engine{:why ::sim-engine/already-created
                                                                 :entity-name :foo-entity
                                                                 :entity-id :uuid-1
                                                                 :bucket 5
                                                                 :entity-state {:foo :bar}}]}}}
      (sut/create #::sim-engine{:entity {:uuid-1 #::sim-engine{:created 4
                                                               :entity-state {:a :b
                                                                              :foo :c}}}}
                  5 :foo-entity
                  :uuid-1 {:foo :bar}))
   "Adding an already existing entity is merging the data map, but created lifecycle is not updated and an error is documented.
Note that `created` `bucket` is not modified on purpose as the real creation has happened before."))

(deftest errors-test
  (is (= {} (sut/errors {})) "No errors returns an empty map.")
  (is (= {:entity-with-error [:list-of-errors]}
         (-> #::sim-engine{:entity {:entity-with-error
                                    #::sim-engine{:created #::sim-engine{:bucket 5}
                                                  :living #::sim-engine{:bucket 5}
                                                  :entity-state {:foo :bar
                                                                 :a :b}
                                                  :errors [:list-of-errors]}
                                    :entity-without-error #::sim-engine{}}}
             sut/errors))
      "Errors of a specific `entity-name` are returned.")
  (is (= {:foo-entity [:error1 :error2]}
         (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:created #::sim-engine{:bucket 5}
                                                              :living #::sim-engine{:bucket 5}
                                                              :entity-state {:foo :bar
                                                                             :a :b}
                                                              :errors [:error1 :error2]}
                                    :ok-entity #::sim-engine{}}}
             sut/errors))
      "Errors are caught in the list, only entities where an error occur."))

(deftest entity-errors-test
  (is (nil? (sut/entity-errors {} :uuid-1)) "A non existing entity has no error")
  (is (= :list-of-errors
         (-> #::sim-engine{:entity {:entity-with-error
                                    #::sim-engine{:created #::sim-engine{:bucket 5}
                                                  :living #::sim-engine{:bucket 5}
                                                  :entity-state {:foo :bar
                                                                 :a :b}
                                                  :errors :list-of-errors}
                                    :entity-without-error #::sim-engine{}}}
             (sut/entity-errors {::sim-engine/entity-id :entity-with-error})))
      "Errors of a specific `entity-id` is returned."))

(def state-stub
  #::sim-engine{:entity {:foo-entity #::sim-engine{:created #::sim-engine{:bucket 3}
                                                   :living #::sim-engine{:bucket 4}
                                                   :entity-state {:data :of
                                                                  :an :entity}}}})

(deftest update-test
  (is
   (= #::sim-engine{:entity {:foo-entity #::sim-engine{:created 3
                                                       :living 5
                                                       :entity-state {:data :of
                                                                      :and :another-data
                                                                      :an :entity}}}}
      (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:created 3
                                                           :living 4
                                                           :entity-state {:data :of
                                                                          :an :entity}}}}
          (sut/update {::sim-engine/entity-id :foo-entity} 5 assoc :and :another-data)))
   "When an existing entity that is living is updated, its entity state and living lifecycle are updated, the created is not modified.")
  (is
   (= #::sim-engine{:entity {:foo-entity
                             #::sim-engine{:created 3
                                           :living 4
                                           :entity-state {:data :of
                                                          :an :entity}
                                           ::sim-engine/errors
                                           [#::sim-engine{:why ::sim-engine/exception-during-update
                                                          :entity-id :foo-entity
                                                          :old-entity #::sim-engine{:created 3
                                                                                    :living 4
                                                                                    :entity-state
                                                                                    {:data :of
                                                                                     :an :entity}}
                                                          :bucket 5
                                                          :args nil}]}}}
      (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:created 3
                                                           :living 4
                                                           :entity-state {:data :of
                                                                          :an :entity}}}}
          (sut/update {::sim-engine/entity-id :foo-entity} 5 #(throw (ex-info "Hey no!" {:a %})))
          (update-in [::sim-engine/entity :foo-entity ::sim-engine/errors 0]
                     dissoc
                     ::sim-engine/exception
                     ::sim-engine/function)))
   "If update is raising an exception, the error is documented, the living date is not, as we consider the update skipped.")
  (is
   (= #::sim-engine{:entity {:foo-entity
                             #::sim-engine{:created 3
                                           :living 12
                                           :entity-state {:data :of
                                                          :an :entity
                                                          :bar :foo}
                                           :disposed 5
                                           :errors
                                           [#::sim-engine{:why
                                                          ::sim-engine/updating-a-disposed-entity
                                                          :bucket 12
                                                          :entity-id :foo-entity
                                                          :old-entity #::sim-engine{:created 3
                                                                                    :living 4
                                                                                    :entity-state
                                                                                    {:data :of
                                                                                     :an :entity}
                                                                                    :disposed 5}
                                                          :function assoc
                                                          :args [:bar :foo]}]}}}
      (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:created 3
                                                           :living 4
                                                           :entity-state {:data :of
                                                                          :an :entity}
                                                           :disposed 5}}}
          (sut/update {::sim-engine/entity-id :foo-entity} 12 assoc :bar :foo)))
   "The update function documents an error if the entity is already disposed, the creation is marked in the lifecycle status.")
  (is (= [#::sim-engine{:why ::sim-engine/updating-a-not-created-entity
                        :entity-id :foo-entity
                        :old-entity nil
                        :bucket 12
                        :function assoc
                        :args [:bar :foo]}]
         (-> {}
             (sut/update {::sim-engine/entity-id :foo-entity} 12 assoc :bar :foo)
             (sut/entity-errors {::sim-engine/entity-id :foo-entity})))
      "Update can update a non existing entity, the creation is marked in the lifecycle status."))

(deftest state-test
  (is (= {:foo :bar}
         (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:created 3
                                                              :living 3
                                                              :entity-state {:foo :bar}}}}
             (sut/state {::sim-engine/entity-id :foo-entity})))
      "The state is returned.")
  (is (nil? (sut/state {} :non-existing-entity)) "No state returned for non existing."))

(deftest dispose-test
  (is (= #::sim-engine{:entity {:foo-entity #::sim-engine{:created 3
                                                          :living 5
                                                          :disposed 10}}}
         (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:entity-state {:data :of
                                                                             :an :entity}
                                                              :created 3
                                                              :living 5}}}
             (sut/dispose {::sim-engine/entity-id :foo-entity} 10)))
      "Disposing an existing entity is updatng the lifecycle and remove its entity-state")
  (is (= {::sim-engine/entity
          {:foo-entity
           #::sim-engine{:created 10
                         :disposed 10
                         :errors [#::sim-engine{:bucket 10
                                                :entity-id :foo-entity
                                                :old-entity nil
                                                :why ::sim-engine/disposing-a-not-created-entity}]
                         :living 10}}}
         (-> #::sim-engine{}
             (sut/dispose {::sim-engine/entity-id :foo-entity} 10)))
      "Disposing a non existing entity creates it and its lifecycle data, and reports an error.")
  (is (= #::sim-engine{:entity {:foo-entity
                                #::sim-engine{:disposed 10
                                              :created 3
                                              :errors
                                              [#::sim-engine{:bucket 10
                                                             :entity-id :foo-entity
                                                             :old-entity #::sim-engine{:disposed 7
                                                                                       :created 3
                                                                                       :living 5}
                                                             :why ::sim-engine/already-disposed}]
                                              :living 5}}}
         (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:disposed 7
                                                              :created 3
                                                              :living 5}}}
             (sut/dispose {::sim-engine/entity-id :foo-entity} 10)))
      "Disposing an already disposed entity reports an error."))

(deftest lifecycle-status-test
  (is (-> {}
          (sut/lifecycle-status :an-non-created-entity)
          empty?)
      "Non existing entity has no lifecycle status.")
  (is
   (= #::sim-engine{:created #::sim-engine{:bucket 3}
                    :living #::sim-engine{:bucket 11}}
      (-> {::sim-engine/entity {:foo-entity #::sim-engine{:created #::sim-engine{:bucket 3}
                                                          :living #::sim-engine{:bucket 11}
                                                          :entity-state {:foo :bar}}}}
          (sut/lifecycle-status {::sim-engine/entity-id :foo-entity})))
   "When the entity is created, its lifecycle-status has its created and living fields at the same date.")
  (is (= #::sim-engine{:created #::sim-engine{:bucket 3}
                       :living #::sim-engine{:bucket 11}}
         (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:created #::sim-engine{:bucket 3}
                                                              :living #::sim-engine{:bucket 11}
                                                              :entity-state {:foo :bar}}}}
             (sut/lifecycle-status {::sim-engine/entity-id :foo-entity})))
      "After update, the lifecycle has a new living date.")
  (is (= #::sim-engine{:created #::sim-engine{:bucket 3}
                       :living #::sim-engine{:bucket 11}
                       :disposed #::sim-engine{:bucket 15}}
         (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:created #::sim-engine{:bucket 3}
                                                              :living #::sim-engine{:bucket 11}
                                                              :disposed #::sim-engine{:bucket 15}
                                                              :entity-state {:foo :bar}}}}
             (sut/lifecycle-status {::sim-engine/entity-id :foo-entity})))
      "After disposed, the lifecycle has a disposed data."))

(deftest is-created?-test
  (is (not (some? (-> {}
                      (sut/is-created? :foo-entity))))
      "A non existing entity is not created.")
  (is (some? (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:created #::sim-engine{:bucket 3}
                                                                  :living #::sim-engine{:bucket
                                                                                        11}}}}
                 (sut/is-created? {::sim-engine/entity-id :foo-entity})))
      "A living entity is created.")
  (is (some? (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:created #::sim-engine{:bucket 3}
                                                                  :living #::sim-engine{:bucket 11}
                                                                  :disposed #::sim-engine{:bucket
                                                                                          15}}}}
                 (sut/is-created? {::sim-engine/entity-id :foo-entity})))
      "A disposed entity is created."))

(deftest is-living?-test
  (is (nil? (-> {}
                (sut/is-living? :foo-entity)))
      "A non existing entity is not living")
  (is (= #::sim-engine{:bucket 11}
         (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:created #::sim-engine{:bucket 3}
                                                              :living #::sim-engine{:bucket 11}}}}
             (sut/is-living? {::sim-engine/entity-id :foo-entity})))
      "A living entity is living")
  (is (nil? (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:created #::sim-engine{:bucket 3}
                                                                 :living #::sim-engine{:bucket 11}
                                                                 :disposed #::sim-engine{:bucket
                                                                                         15}}}}
                (sut/is-living? {::sim-engine/entity-id :foo-entity})))
      "A disposed entity is not living."))

(deftest is-disposed?-test
  (is (nil? (-> {}
                (sut/is-disposed? {::sim-engine/entity-id :foo-entity})))
      "A non existing entity is not disposed.")
  (is (nil? (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:created #::sim-engine{:bucket 3}
                                                                 :living #::sim-engine{:bucket
                                                                                       11}}}}
                (sut/is-disposed? {::sim-engine/entity-id :foo-entity})))
      "A living entity is not disposed.")
  (is (= #::sim-engine{:bucket 15}
         (-> #::sim-engine{:entity {:foo-entity #::sim-engine{:created #::sim-engine{:bucket 3}
                                                              :living #::sim-engine{:bucket 11}
                                                              :disposed #::sim-engine{:bucket 15}}}}
             (sut/is-disposed? {::sim-engine/entity-id :foo-entity})))
      "A disposed entity is disposed."))

;; ********************************************************************************
;;; Assembly tests
;; ********************************************************************************

(def state0 {})

(def state1
  (-> state0
      (sut/create 3 :my-first-entity
                  :uuid-1 {:data :of
                           :an :entity})))

(def state2
  (-> state1
      (sut/update {::sim-engine/entity-id :uuid-1} 5 assoc :and :another-data)))

(def state3
  (-> state2
      (sut/dispose {::sim-engine/entity-id :uuid-1} 12)))

(deftest assembly-tests
  (is (nil? (sut/is-created? :non-existing state1)))
  (is (nil? (sut/is-living? :non-existing state1)))
  (is (nil? (sut/is-disposed? :non-existing state1)))
  (is (= 3 (sut/is-created? state1 {::sim-engine/entity-id :uuid-1})))
  (is (= 3 (sut/is-living? state1 {::sim-engine/entity-id :uuid-1})))
  (is (nil? (sut/is-disposed? state1 {::sim-engine/entity-id :uuid-1})))
  (is (= {:data :of
          :an :entity
          :and :another-data}
         (sut/state state2 {::sim-engine/entity-id :uuid-1})))
  (is (= 5 (sut/is-living? state2 {::sim-engine/entity-id :uuid-1})))
  (is (= {:data :of
          :an :entity
          :and :another-data}
         (-> state2
             (sut/state {::sim-engine/entity-id :uuid-1}))))
  (is (= nil
         (-> state3
             (sut/state {::sim-engine/entity-id :uuid-1}))))
  (is (= nil
         (-> state3
             (sut/is-living? {::sim-engine/entity-id :uuid-1}))))
  (is (= 12
         (-> state3
             (sut/is-disposed? {::sim-engine/entity-id :uuid-1})))))

;; ********************************************************************************
;; Event API
;; ********************************************************************************
(deftest schedule-test
  (is (= #::sim-engine{:future-events [#::sim-engine{:event :data
                                                     :entity-id :entity-uuid-1
                                                     :bucket 3}]}
         (sut/schedule nil {::sim-engine/entity-id :entity-uuid-1} 3 {::sim-engine/event :data}))
      "Copy entity id"))
