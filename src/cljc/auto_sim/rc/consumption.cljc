(ns auto-sim.rc.consumption
  "A consumption is an entity which lifecycle starts with a `seizing-event` seizing a resource and ends with a `free`ing that resource.

  A consumption has a `consumption-uuid` created to identify this particular consumption.

  Informations are stored in the `resource` under the `::sim-rc/currently-consuming` key."
  (:require
   [auto-core.uuid :as uuid-gen]
   [auto-sim.rc    :as-alias sim-rc]))

(defn consume
  "Consume `consumed-quantity` number of `resource`, store this consuming informations in the `currently-consuming` attribute of the resource.

  Returns a pair with:
     * the `consumption-uuid` - generated
     * `resource` with `currently-consuming` updated with an entry under `consumption-uuid` entry with a map consisting in the two following entries:
        * `seizing-event` the event that has triggered a seizing.
        * `consumed-quantity` number consumed."
  [resource consumed-quantity seizing-event]
  (if (some? seizing-event)
    (let [consumption-uuid (uuid-gen/time-based-uuid)]
      [consumption-uuid
       (update resource
               ::sim-rc/currently-consuming
               assoc
               consumption-uuid
               #::sim-rc{:seizing-event seizing-event
                         :consumed-quantity consumed-quantity})])
    [nil resource]))

(defn free
  "Remove the seizing informations matching `consumption-uuid` for that resource.
  Returns the updated resource without that `consumption` anymore."
  [resource consumption-uuid]
  (update resource
          ::sim-rc/currently-consuming
          (fn [currently-consuming]
            (if (nil? currently-consuming) {} (dissoc currently-consuming consumption-uuid)))))
