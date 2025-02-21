(ns auto-sim.rc.resource.consumption
  "A `consumption` respresents the lifecycle of an `auto-sim.entity` seizing a `resource`.

  It starts when the `resource` has enough items available for the entity with the `start` event.
  It ends with an event called `ended` which happens when a `resource` is disposed by its entity.

  A consumption is identified called `consumption-uuid`.
  All related informations are stored in the `resource` under the `::auto-sim.engine/consumption` key."
  (:require
   [auto-core.uuid  :as uuid-gen]
   [auto-sim.engine :as-alias sim-engine]))

(defn start
  "Consume `quantity` items of `resource`, store these informations in the `:auto-sim.engine/consumption` attribute of the resource.

  Returns a map with:
     * `consumption-uuid` - an uuid generated
     * `resource` with `consumption` updated with an entry under `consumption-uuid` entry with a map consisting in the following entries:
        * `event` the event that has triggered a seizing, it should be an entity and have an `entity-id`
        * `quantity` number consumed.
        * `errors` non `nil` when the `entity-id` is not found in the `event`"
  [resource event quantity priority]
  (let [{::sim-engine/keys [entity-id]} event]
    (cond
      (nil? entity-id) {:resource (if (empty? resource) {} resource)
                        :errors [#::sim-engine{:why :event-miss-entity-id
                                               :consumption-quantity quantity}]}
      (not (and (integer? quantity) (pos? quantity)))
      {:resource (if (empty? resource) {} resource)
       :errors [#::sim-engine{:why :consumption-quantity-wrong
                              :resource resource
                              :consumption-quantity quantity}]}
      :else (let [consumption-uuid (uuid-gen/time-based-uuid)]
              {:consumption-uuid consumption-uuid
               :resource (assoc-in resource
                          [::sim-engine/consumption consumption-uuid]
                          #::sim-engine{:entity-id entity-id
                                        :priority priority
                                        :consumption-quantity quantity})}))))

(defn ended
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

  First element in the order will be sorted first in a collection sorted with this comparator"
  [order]
  (fn [a b]
    (cond
      (nil? a) 666
      (nil? b) -666
      (not= a b) (- (.indexOf order a) (.indexOf order b))
      :else 0)))

(defn consumption-by-priority
  "Returns `consumptions` from a `resource` ordered with their priority thanks to `priority-comparator`"
  [resource priority-comparator]
  (->> (::sim-engine/consumption resource)
       (sort-by (comp ::sim-engine/priority second) priority-comparator)
       vec))
