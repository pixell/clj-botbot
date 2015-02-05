(ns clj-botbot.flowdock.flow
  (:require [clojure.tools.logging :as log]
            [clj-flowdock.api.flow :as api-flow]
            [clj-flowdock.streaming :as stream]))

(use 'clojure.tools.trace)

(def flows (api-flow/list-all true))

(defn stream [private & flow-ids]
  (apply stream/open private flow-ids))

(defn read-flow [flow-conn]
  (let [data (.read flow-conn)]
    (log/info "Data:" data)
    data))

(defn get-flows []
  ; Cleanup a little bit
  (let [extract-id #(assoc %1 (%2 "id") %2)
        users-processed (map #(assoc %1 "users" (reduce extract-id {} (%1 "users"))) flows)
        flows-processed (reduce extract-id {} users-processed)]
    flows-processed))

(defn get-org-flow-id [flow-id]
  (->>
    flows
    (filter #(= flow-id (% "id")))
    first
    (api-flow/flow->flow-id)))

(deftrace get-user [flow-id user-id]
  (->>
    flows
    (filter #(= flow-id (% "id")))
    first
    (#(% "users"))
    (filter #(= user-id (str (% "id"))))
    first))
