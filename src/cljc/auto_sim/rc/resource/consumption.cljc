(ns auto-sim.rc.resource.consumption
  "A `consumption` respresents the lifecycle of an `auto-sim.entity`.

  It starts with an event called `consume` which happens when a `resource` has enough available items during a `seize`.
  It ends with an event called `free` which happens when a `resource` is disposed by its entity.

  A consumption is identified called `consumption-uuid`. Informations are stored in the `resource` under the `::auto-sim.engine/consumption` key."
  (:require
   [auto-core.uuid  :as uuid-gen]
   [auto-sim.engine :as-alias sim-engine]))

(defn consume
  "Consume `consumption-quantity` items of `resource`, store these informations in the `:auto-sim.engine/consumption` attribute of the resource.

  Returns a map with:
     * `consumption-uuid` - an uuid generated
     * `resource` with `consumption` updated with an entry under `consumption-uuid` entry with a map consisting in the two following entries:
        * `event` the event that has triggered a seizing.
        * `consumption-quantity` number consumed.
        * `errors` non `nil` when the `entity-id` is not found in the `event`"
  [resource event consumption-quantity priority]
  (let [{::sim-engine/keys [entity-id]} event]
    (cond
      (nil? entity-id) {:resource (if (empty? resource) {} resource)
                        :errors [#::sim-engine{:why :consume-has-no-event-id
                                               :event event
                                               :resource resource
                                               :consumption-quantity consumption-quantity}]}
      (not (and (integer? consumption-quantity) (pos? consumption-quantity)))
      {:resource (if (empty? resource) {} resource)
       :errors [#::sim-engine{:why :consumption-quantity-wrong
                              :event event
                              :resource resource
                              :consumption-quantity consumption-quantity}]}
      :else (let [consumption-uuid (uuid-gen/time-based-uuid)]
              {:consumption-uuid consumption-uuid
               :resource (assoc-in resource
                          [::sim-engine/consumption consumption-uuid]
                          #::sim-engine{:entity-id entity-id
                                        :priority priority
                                        :consumption-quantity consumption-quantity})}))))

(defn free
  "Remove the seizing informations matching `consumption-uuid` for that resource.

  Returns a map with
  * `resource` without that `consumption` anymore.
  * `errors` if `consumption-uuid` is missing"
  [resource consumption-uuid]
  (if (nil? (get-in resource [::sim-engine/consumption consumption-uuid]))
    {:resource resource
     :errors [#::sim-engine{:why :consumption-uuid-does-not-exist
                            :resource resource
                            :consumption-uuid consumption-uuid}]}
    {:resource (-> resource
                   (update ::sim-engine/consumption
                           (fn [consumption]
                             (if (nil? consumption) {} (dissoc consumption consumption-uuid)))))}))

(defn compare-by-order
  "Returns a comparator that implement java.util.Comparator.

  Firs element in the order will be sorted first in a collection sorted with this comparator"
  [order]
  (fn [a b]
    (cond
      (nil? a) 666
      (nil? b) -666
      (not= a b) (- (.indexOf order a) (.indexOf order b))
      :else 0)))

(defn consumption-by-priority
  "Returns `consumptions` from a `resource` concerning entity `entity-id`.

  Consumptions are ordered with their priority, ordered with `priority-comparator`"
  [resource entity-id priority-comparator]
  (->> (::sim-engine/consumption resource)
       (filter #(= entity-id
                   (-> %
                       second
                       ::sim-engine/entity-id)))
       (sort-by (comp ::sim-engine/priority second) priority-comparator)
       vec))
