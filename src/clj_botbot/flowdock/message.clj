(ns clj-botbot.flowdock.message
  (:require [clojure.tools.logging :as log]
            [clj-botbot.flowdock.flow :as flow]
            [clj-flowdock.api :as api]
            [clj-flowdock.api.message :as api-message]
            [clojure.core.match :refer [match]]))

(def accepted-events #{"comment" "message" "message-edit"})

(defn get-content [msg]
  (match [(msg "event")]
    ["comment"] (get-in msg ["content" "text"])
    ["message"] (msg "content")
    ["message-edit"]
      ; message-edit is also used when editing comments.
      (let [content (get-in msg ["content" "updated_content"])]
        (if (map? content)
          (content "text")
          content))))

(defn get-content-only [msg]
  (->>
    (get-content msg)
    (re-matches #"(?i)\s*(@\w+\s*[,:]?)?\s*(.*)")
    last))

(defn get-org-flow-id [msg]
  (flow/get-org-flow-id (msg "flow")))

(defn get-message-id [msg]
  (match [(msg "event")]
    ["comment"] (api-message/parent-message-id msg)
    ["message"] (msg "id")
    ["message-edit"]
      ; If edited item is comment (has "content" / "updated_content" / "title"),
      ; to get correct message id we need to fetch actual comment ahd check for
      ; influx tag in it
      (let [msg-id (get-in msg ["content" "message"])]
        (if (nil? (get-in msg ["content" "updated_content" "title"]))
          msg-id
          (->> msg-id
            (api-message/get-message (get-org-flow-id msg))
            (api-message/parent-message-id))))))

(defn get-sender-nick [msg]
  (let [flow-id (msg "flow")
        user-id (msg "user")
        user (flow/get-user flow-id user-id)]
    (when-not (nil? user)
      (user "nick"))))

(defn is-private [msg]
  (not (nil? (msg "to"))))

(defn respond [msg response]
  (if (is-private msg)
    ; Private.
    (api-message/send-private-message (msg "user") response)
    ; Public.
    (let [flow-id (get-org-flow-id msg)
          content (str "@" (get-sender-nick msg) ", " response)
          msg-id  (get-message-id msg)
          message {:event "comment" :content content}]
      (api/http-post (str "flows/" flow-id "/messages/" msg-id "/comments") message))))

(defn is-user-in-tags [user-id msg]
  (let [user (str ":user:" user-id)]
    (some (partial = user) (msg "tags"))))

(defn is-sent-by-me [user-id msg]
  (= user-id (msg "user")))

(defn is-sent-to-me [user-id msg]
  (or
    (= (msg "to") user-id)
    (is-user-in-tags user-id msg)
    (when (= "message-edit" (msg "event"))
      (->>
        (api-message/get-message (get-org-flow-id msg) (get-in msg ["content" "message"]))
        (is-user-in-tags user-id)))))
