(ns auto-sim.rc.impl.unblocking-policy.registry
  "Registry for `unblocking-policy`."
  (:require
   [auto-sim.rc                             :as-alias sim-rc]
   [auto-sim.rc.impl.unblocking-policy.base :as sim-de-rc-unblocking-policy-base]
   [auto-sim.rc.unblocking-policy           :as sim-de-rc-unblocking-policy]))

(defn schema
  "Schema of an `unblocking-policy` registry."
  []
  [:map-of :keyword (sim-de-rc-unblocking-policy/schema)])

(defn registry
  "The base policies `registry`."
  []
  #:auto-sim.rc{:FIFO sim-de-rc-unblocking-policy-base/fifo-policy
                               :LIFO sim-de-rc-unblocking-policy-base/lifo-policy})
