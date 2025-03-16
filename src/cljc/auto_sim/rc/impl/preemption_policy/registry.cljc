(ns auto-sim.rc.impl.preemption-policy.registry
  "Registry for `preemption-policy`."
  (:require
   [auto-sim.rc                             :as-alias sim-rc]
   [auto-sim.rc.impl.preemption-policy.base :as sim-de-rc-preemption-policy-base]
   [auto-sim.rc.preemption-policy           :as sim-de-rc-preemption-policy]))

(defn schema
  "Schema for a `preemption-policy` registry."
  []
  [:map-of :keyword (sim-de-rc-preemption-policy/schema)])

(defn registry
  "The base policies for `preemption-policy`."
  []
  #:auto-sim.rc{::sim-rc/no-preemption sim-de-rc-preemption-policy-base/no-preemption})
