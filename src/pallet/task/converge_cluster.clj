(ns pallet.task.converge-cluster
  "Adjust node counts for a cluster."
  (:require
   [clojure.tools.logging :as logging])
  (:use
   [pallet.api :only [converge]]))

(defn- build-args [args]
  (loop [args args
         prefix nil
         m nil
         phases []]
    (if-let [a (first args)]
      (cond
       (and (nil? m) (symbol? a) (nil? (namespace a))) (recur
                                                        (next args)
                                                        (name a)
                                                        m
                                                        phases)
       (nil? m) (recur (next args) prefix a phases)
       :else (recur (next args) prefix m (conj phases a)))
      (concat [m] (if prefix [:prefix prefix] []) [:phase phases]))))

(defn converge-cluster
  "Adjust node counts of a cluster.  Requires the name of the cluster.
       pallet converge-cluster org.mynodes/my-cluster
       pallet converge-cluster org.mynodes/my-cluster :install :configure
   The cluster name should be namespace qualified."
  [request & args]
  (let [args (build-args args)]
    (apply converge-cluster
           (concat args
                   (apply concat
                          (->
                           request
                           (dissoc :config :project)
                           (assoc :environment
                             (or (:environment request)
                                 (-> request :project :environment)))))))))
