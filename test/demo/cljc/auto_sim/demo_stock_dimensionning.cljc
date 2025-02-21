(ns auto-sim.demo-stock-dimensionning
  "Stock dimensionning models regular items getting in your workshop.

  We generate products in proportion of field quantities.

  By measuring the size of products in input stocks, we can measure the bottleneck is.
  Of course, it may sound unreal to have too many, but in reality your team will slow down the workflow for such products.

  Why stock number of slots are not modelled: As in practise, situations where a machine is stuck as there is no place"
  (:require
   [auto-sim.engine :as sim-engine]))

(def model-data
  #::sim-engine{:starting-bucket 0
                :max-nb-entity 4
                :waiting {:time 100
                          :type :exponential}
                :products {:parts-1 {:route [{:m [:tvm-1 :tvm-2 :tvm-3]
                                              :is 2
                                              :os 2
                                              :pt {:law :normal
                                                   :location 12
                                                   :scale 2}}
                                             {:m :orc
                                              :is 2
                                              :os 2
                                              :pt {:law :normal
                                                   :location 12
                                                   :scale 2}}
                                             {:m [:lvt-1 :lvt-2 :lvt-3]
                                              :is 2
                                              :os 2
                                              :pt {:law :normal
                                                   :location 12
                                                   :scale 2}}]
                                     :quantity 100}
                           :parts-2 {:route
                                     [{:m [:cn5-1 :cn5-2 :cn5-3 :cn5-4 :cn5-5 :cn5-6 :cn5-7 :cn5-8]
                                       :pt {:law :normal
                                            :location 12
                                            :scale 2}}]
                                     :quantity 100}
                           :parts-3 {:route [{:m [:phib-1 :phib2]
                                              :pt {:law :normal
                                                   :location 12
                                                   :scale 2}}
                                             {:m :hf
                                              :pt {:law :normal
                                                   :location 12
                                                   :scale 2}}
                                             {:m :psk
                                              :pt {:law :normal
                                                   :location 12
                                                   :scale 2}}]
                                     :quantity 100}
                           :csp {:route [{:m [:lvt :tfm]
                                          :pt {:law :normal
                                               :location 12
                                               :scale 2}}
                                         {:m [:dmc-1 :dmc-2]
                                          :pt {:law :normal
                                               :location 12
                                               :scale 2}}
                                         {:m :hf
                                          :pt {:law :normal
                                               :location 12
                                               :scale 2}}]
                                 :quantity 100}}
                ::seed #uuid "e85427c1-ed25-4ed4-9b11-52238d268265"})

{:max-nb-entity 4
 :routes {:anneau [{:is 2 ;; Number of stock slots
                    :m [:tvm-1 :tvm-2 :tvm-3] ;; machines
                    :os 2 ;; Numbers of stock slots
                    :pt {:law :normal ;; Distribution of processing times
                         :location 12 ;; Mean value of the normal law
                         :scale 2     ;; Variance of the normal law
                        }}
                   {:is 2
                    :m :orc
                    :os 2
                    :pt {:law :normal
                         :location 12
                         :scale 2}}
                   {:is 2
                    :m [:lvt-1 :lvt-2 :lvt-3]
                    :os 2
                    :pt {:law :normal
                         :location 12
                         :scale 2}}]
          :csp [{:m [:lvt :tfm]
                 :pt {:law :normal
                      :location 12
                      :scale 2}}
                {:m [:dmc-1 :dmc-2]
                 :pt {:law :normal
                      :location 12
                      :scale 2}}
                {:m :hf
                 :pt {:law :normal
                      :location 12
                      :scale 2}}]}
 :waiting {:type :exponential ;; Probabilistic distribution law
           :location 10 ;; Average
           :scale 2 ;; Standard
          }}
