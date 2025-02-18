(ns auto-sim.rc.preemption-policy "Basic preemption policies.")

(defn no-preemption
  "Do nothing when a preemption occurs. In other words ,the failure or capacity decreases will have no effect before the next event on that resource."
  [resource]
  {:preempts []
   :resource resource})
