(ns auto-sim.rc.state
  "Store and update resource consumer informations in the `::sim-rc/resource` key of the state.
  Assuming state is associative."
  (:require
   [auto-sim.rc          :as-alias sim-rc]
   [auto-sim.rc.resource :as sim-rc-resource]))

(defn- update-resource
  [state resource-name resource]
  (cond-> state
    (some? resource-name) (assoc-in [::sim-rc/resource resource-name] resource)))

(defn update-resource-capacity
  "Returns a pair of:
  * `unblocked-events`
  * `state` where the resource called `resource-name` is set to its new capacity `new-capacity`."
  [state resource-name new-capacity preemption-policy-fn unblocking-policy-fn]
  (let [resource (get-in state [::sim-rc/resource resource-name])
        [unblocked-events resource] (sim-rc-resource/update-capacity resource
                                                                     new-capacity
                                                                     preemption-policy-fn
                                                                     unblocking-policy-fn)]
    [unblocked-events (update-resource state resource-name resource)]))

(defn dispose
  "Dispose the resource `resource-name` for `consumption-uuid`.

  Returns a pair:
  * the `unblockings` event.
  * the `state`, with the resource `resource-name` consumption of `seizing-event` is disposed."
  [state resource-name consumption-uuid unblocking-policy-fn]
  (if (or (nil? consumption-uuid) (nil? resource-name))
    [[] state]
    (let [resource (get-in state [::sim-rc/resource resource-name])
          [unblockings resource]
          (sim-rc-resource/dispose resource consumption-uuid unblocking-policy-fn)]
      [unblockings (update-resource state resource-name resource)])))
