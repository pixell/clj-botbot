(ns clj-botbot.core
  (:require [clojure.tools.logging :as log]
            [clj-botbot.flowdock.flow :as flow]
            [clj-botbot.flowdock.message :as message]
            [clj-botbot.commands.jenkins :as jenkins]
            [clj-botbot.commands.jira :as jira])
  (:gen-class))

(def handlers (atom []))

(defn add-handlers [& new-handlers]
  (swap! handlers #(into % (flatten new-handlers))))

(defn- process-msg [config msg]
  (dorun (map #(%1 config msg) (deref handlers))))

(defn- receive [conn user-id msg]
  (when (and (not (nil? msg))
             (not (message/is-sent-by-me user-id msg))
             (contains? message/accepted-events (msg "event")))
      (process-msg {"conn" conn
                    "to-me" (message/is-sent-to-me user-id msg)}
                   msg)))

(defn run
  ([flow-ids] (run 0 (first flow-ids) (next flow-ids)))
  ([message-limit flow-id & flow-ids]
    (let [conn (apply flow/stream true (conj flow-ids flow-id))
          user-id (.user-id conn)]
      (loop [msg (flow/read-flow conn)
             processed 0]
          (receive conn user-id msg)
        (when (or (zero? message-limit) (< processed message-limit))
          (recur (flow/read-flow conn) (inc processed)))))))

(defn -main [& args]
  (reset! handlers [])
  (add-handlers jira/bot-handlers)
  (apply run 0 args))
