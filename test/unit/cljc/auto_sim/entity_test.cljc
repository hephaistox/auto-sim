(ns auto-sim.entity-test
  (:require
   #?(:clj [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer [deftest is] :include-macros true])
   [auto-sim.engine :as-alias sim-engine]
   [auto-sim.entity :as sut]))

(deftest create-test
  (is (= #::sut{:entity {:uuid-1 #::sut{:created 3
                                        :living 3
                                        :entity-state {:foo :bar}}}}
         (sut/create {} 3 :foo-entity :uuid-1 {:foo :bar}))
      "Adding a new entity that was not existing before.")
  (is
   (= {::sut/entity {:uuid-1 #::sut{:created 4
                                    :entity-state {:foo :c
                                                   :a :b}
                                    :errors [#::sut{:why ::sut/already-created
                                                    :entity-name :foo-entity
                                                    :entity-id :uuid-1
                                                    :bucket 5
                                                    :entity-state {:foo :bar}}]}}}
      (sut/create #::sut{:entity {:uuid-1 #::sut{:created 4
                                                 :entity-state {:a :b
                                                                :foo :c}}}}
                  5 :foo-entity
                  :uuid-1 {:foo :bar}))
   "Adding an already existing entity is merging the data map, but created lifecycle is not updated and an error is documented.
Note that `created` `bucket` is not modified on purpose as the real creation has happened before."))

(deftest errors-test
  (is (= {} (sut/errors {})) "No errors returns an empty map.")
  (is (= {:entity-with-error [:list-of-errors]}
         (-> #::sut{:entity {:entity-with-error #::sut{:created #::sut{:bucket 5}
                                                       :living #::sut{:bucket 5}
                                                       :entity-state {:foo :bar
                                                                      :a :b}
                                                       :errors [:list-of-errors]}
                             :entity-without-error #::sut{}}}
             sut/errors))
      "Errors of a specific `entity-name` are returned.")
  (is (= {:foo-entity [:error1 :error2]}
         (-> #::sut{:entity {:foo-entity #::sut{:created #::sut{:bucket 5}
                                                :living #::sut{:bucket 5}
                                                :entity-state {:foo :bar
                                                               :a :b}
                                                :errors [:error1 :error2]}
                             :ok-entity #::sut{}}}
             sut/errors))
      "Errors are caught in the list, only entities where an error occur."))

(deftest entity-errors-test
  (is (nil? (sut/entity-errors {} :uuid-1)) "A non existing entity has no error")
  (is (= :list-of-errors
         (-> #::sut{:entity {:entity-with-error #::sut{:created #::sut{:bucket 5}
                                                       :living #::sut{:bucket 5}
                                                       :entity-state {:foo :bar
                                                                      :a :b}
                                                       :errors :list-of-errors}
                             :entity-without-error #::sut{}}}
             (sut/entity-errors {::sut/entity-id :entity-with-error})))
      "Errors of a specific `entity-id` is returned."))

(def state-stub
  #::sut{:entity {:foo-entity #::sut{:created #::sut{:bucket 3}
                                     :living #::sut{:bucket 4}
                                     :entity-state {:data :of
                                                    :an :entity}}}})

(deftest update-test
  (is
   (= #::sut{:entity {:foo-entity #::sut{:created 3
                                         :living 5
                                         :entity-state {:data :of
                                                        :and :another-data
                                                        :an :entity}}}}
      (-> #::sut{:entity {:foo-entity #::sut{:created 3
                                             :living 4
                                             :entity-state {:data :of
                                                            :an :entity}}}}
          (sut/update 5 {::sut/entity-id :foo-entity} assoc :and :another-data)))
   "When an existing entity that is living is updated, its entity state and living lifecycle are updated, the created is not modified.")
  (is
   (=
    #::sut{:entity {:foo-entity #::sut{:created 3
                                       :living 4
                                       :entity-state {:data :of
                                                      :an :entity}
                                       ::sut/errors [#::sut{:why ::sut/exception-during-update
                                                            :entity-id :foo-entity
                                                            :old-entity #::sut{:created 3
                                                                               :living 4
                                                                               :entity-state
                                                                               {:data :of
                                                                                :an :entity}}
                                                            :bucket 5
                                                            :args nil}]}}}
    (->
      #::sut{:entity {:foo-entity #::sut{:created 3
                                         :living 4
                                         :entity-state {:data :of
                                                        :an :entity}}}}
      (sut/update 5 {::sut/entity-id :foo-entity} #(throw (ex-info "Hey no!" {:a %})))
      (update-in [::sut/entity :foo-entity ::sut/errors 0] dissoc ::sut/exception ::sut/function)))
   "If update is raising an exception, the error is documented, the living date is not, as we consider the update skipped.")
  (is
   (= #::sut{:entity {:foo-entity #::sut{:created 3
                                         :living 12
                                         :entity-state {:data :of
                                                        :an :entity
                                                        :bar :foo}
                                         :disposed 5
                                         :errors
                                         [#::sut{:why :auto-sim.entity/updating-a-disposed-entity
                                                 :bucket 12
                                                 :entity-id :foo-entity
                                                 :old-entity #::sut{:created 3
                                                                    :living 4
                                                                    :entity-state {:data :of
                                                                                   :an :entity}
                                                                    :disposed 5}
                                                 :function assoc
                                                 :args [:bar :foo]}]}}}
      (-> #::sut{:entity {:foo-entity #::sut{:created 3
                                             :living 4
                                             :entity-state {:data :of
                                                            :an :entity}
                                             :disposed 5}}}
          (sut/update 12 {::sut/entity-id :foo-entity} assoc :bar :foo)))
   "The update function documents an error if the entity is already disposed, the creation is marked in the lifecycle status.")
  (is (= [#::sut{:why ::sut/updating-a-not-created-entity
                 :entity-id :foo-entity
                 :old-entity nil
                 :bucket 12
                 :function assoc
                 :args [:bar :foo]}]
         (-> {}
             (sut/update 12 {::sut/entity-id :foo-entity} assoc :bar :foo)
             (sut/entity-errors {::sut/entity-id :foo-entity})))
      "Update can update a non existing entity, the creation is marked in the lifecycle status."))

(deftest state-test
  (is (= {:foo :bar}
         (-> #::sut{:entity {:foo-entity #::sut{:created 3
                                                :living 3
                                                :entity-state {:foo :bar}}}}
             (sut/state {::sut/entity-id :foo-entity})))
      "The state is returned.")
  (is (nil? (sut/state {} :non-existing-entity)) "No state returned for non existing."))

(deftest dispose-test
  (is (= #::sut{:entity {:foo-entity #::sut{:created 3
                                            :living 5
                                            :disposed 10}}}
         (-> #::sut{:entity {:foo-entity #::sut{:entity-state {:data :of
                                                               :an :entity}
                                                :created 3
                                                :living 5}}}
             (sut/dispose 10 {::sut/entity-id :foo-entity})))
      "Disposing an existing entity is updatng the lifecycle and remove its entity-state")
  (is (= {::sut/entity {:foo-entity #::sut{:created 10
                                           :disposed 10
                                           :errors [#::sut{:bucket 10
                                                           :entity-id :foo-entity
                                                           :old-entity nil
                                                           :why
                                                           ::sut/disposing-a-not-created-entity}]
                                           :living 10}}}
         (-> #::sut{}
             (sut/dispose 10 {::sut/entity-id :foo-entity})))
      "Disposing a non existing entity creates it and its lifecycle data, and reports an error.")
  (is (= #::sut{:entity {:foo-entity #::sut{:disposed 10
                                            :created 3
                                            :errors [#::sut{:bucket 10
                                                            :entity-id :foo-entity
                                                            :old-entity #::sut{:disposed 7
                                                                               :created 3
                                                                               :living 5}
                                                            :why ::sut/already-disposed}]
                                            :living 5}}}
         (-> #::sut{:entity {:foo-entity #::sut{:disposed 7
                                                :created 3
                                                :living 5}}}
             (sut/dispose 10 {::sut/entity-id :foo-entity})))
      "Disposing an already disposed entity reports an error."))

(deftest lifecycle-status-test
  (is (-> {}
          (sut/lifecycle-status :an-non-created-entity)
          empty?)
      "Non existing entity has no lifecycle status.")
  (is
   (= #::sut{:created #::sut{:bucket 3}
             :living #::sut{:bucket 11}}
      (-> {::sut/entity {:foo-entity #::sut{:created #::sut{:bucket 3}
                                            :living #::sut{:bucket 11}
                                            :entity-state {:foo :bar}}}}
          (sut/lifecycle-status {::sut/entity-id :foo-entity})))
   "When the entity is created, its lifecycle-status has its created and living fields at the same date.")
  (is (= #::sut{:created #::sut{:bucket 3}
                :living #::sut{:bucket 11}}
         (-> #::sut{:entity {:foo-entity #::sut{:created #::sut{:bucket 3}
                                                :living #::sut{:bucket 11}
                                                :entity-state {:foo :bar}}}}
             (sut/lifecycle-status {::sut/entity-id :foo-entity})))
      "After update, the lifecycle has a new living date.")
  (is (= #::sut{:created #::sut{:bucket 3}
                :living #::sut{:bucket 11}
                :disposed #::sut{:bucket 15}}
         (-> #::sut{:entity {:foo-entity #::sut{:created #::sut{:bucket 3}
                                                :living #::sut{:bucket 11}
                                                :disposed #::sut{:bucket 15}
                                                :entity-state {:foo :bar}}}}
             (sut/lifecycle-status {::sut/entity-id :foo-entity})))
      "After disposed, the lifecycle has a disposed data."))

(deftest is-created?-test
  (is (not (some? (-> {}
                      (sut/is-created? :foo-entity))))
      "A non existing entity is not created.")
  (is (some? (-> #::sut{:entity {:foo-entity #::sut{:created #::sut{:bucket 3}
                                                    :living #::sut{:bucket 11}}}}
                 (sut/is-created? {::sut/entity-id :foo-entity})))
      "A living entity is created.")
  (is (some? (-> #::sut{:entity {:foo-entity #::sut{:created #::sut{:bucket 3}
                                                    :living #::sut{:bucket 11}
                                                    :disposed #::sut{:bucket 15}}}}
                 (sut/is-created? {::sut/entity-id :foo-entity})))
      "A disposed entity is created."))

(deftest is-living?-test
  (is (nil? (-> {}
                (sut/is-living? :foo-entity)))
      "A non existing entity is not living")
  (is (= #::sut{:bucket 11}
         (-> #::sut{:entity {:foo-entity #::sut{:created #::sut{:bucket 3}
                                                :living #::sut{:bucket 11}}}}
             (sut/is-living? {::sut/entity-id :foo-entity})))
      "A living entity is living")
  (is (nil? (-> #::sut{:entity {:foo-entity #::sut{:created #::sut{:bucket 3}
                                                   :living #::sut{:bucket 11}
                                                   :disposed #::sut{:bucket 15}}}}
                (sut/is-living? {::sut/entity-id :foo-entity})))
      "A disposed entity is not living."))

(deftest is-disposed?-test
  (is (nil? (-> {}
                (sut/is-disposed? {::sut/entity-id :foo-entity})))
      "A non existing entity is not disposed.")
  (is (nil? (-> #::sut{:entity {:foo-entity #::sut{:created #::sut{:bucket 3}
                                                   :living #::sut{:bucket 11}}}}
                (sut/is-disposed? {::sut/entity-id :foo-entity})))
      "A living entity is not disposed.")
  (is (= #::sut{:bucket 15}
         (-> #::sut{:entity {:foo-entity #::sut{:created #::sut{:bucket 3}
                                                :living #::sut{:bucket 11}
                                                :disposed #::sut{:bucket 15}}}}
             (sut/is-disposed? {::sut/entity-id :foo-entity})))
      "A disposed entity is disposed."))

;;; Assembly tests
;;; ****************

(def state0 {})

(def state1
  (-> state0
      (sut/create 3 :my-first-entity
                  :uuid-1 {:data :of
                           :an :entity})))

(def state2
  (-> state1
      (sut/update 5 {::sut/entity-id :uuid-1} assoc :and :another-data)))

(def state3
  (-> state2
      (sut/dispose 12 {::sut/entity-id :uuid-1})))

(deftest assembly-tests
  (is (nil? (sut/is-created? :non-existing state1)))
  (is (nil? (sut/is-living? :non-existing state1)))
  (is (nil? (sut/is-disposed? :non-existing state1)))
  (is (= 3 (sut/is-created? state1 {::sut/entity-id :uuid-1})))
  (is (= 3 (sut/is-living? state1 {::sut/entity-id :uuid-1})))
  (is (nil? (sut/is-disposed? state1 {::sut/entity-id :uuid-1})))
  (is (= {:data :of
          :an :entity
          :and :another-data}
         (sut/state state2 {::sut/entity-id :uuid-1})))
  (is (= 5 (sut/is-living? state2 {::sut/entity-id :uuid-1})))
  (is (= {:data :of
          :an :entity
          :and :another-data}
         (-> state2
             (sut/state {::sut/entity-id :uuid-1}))))
  (is (= nil
         (-> state3
             (sut/state {::sut/entity-id :uuid-1}))))
  (is (= nil
         (-> state3
             (sut/is-living? {::sut/entity-id :uuid-1}))))
  (is (= 12
         (-> state3
             (sut/is-disposed? {::sut/entity-id :uuid-1})))))
