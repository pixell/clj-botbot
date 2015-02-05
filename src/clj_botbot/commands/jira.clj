(ns clj-botbot.commands.jira
  (:require [clojure.tools.logging :as log]
            [clj-botbot.flowdock.message :as message]
            [clj-http.client :as client]
            [cheshire.core :as json])
  (:refer-clojure :exclude [path])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(def base-url
  (or (System/getProperty "JIRA_URL") "http://localhost:8080"))

(def basic-auth
  [(System/getProperty "JIRA_USERNAME")
   (System/getProperty "JIRA_PASSWORD")])

(defn get-url
  ([path] (get-url path {}))
  ([path query-params]
    (log/info (str base-url path))
    (:body (client/get
            (str base-url path)
            {:as :json-string-keys
             :basic-auth basic-auth
             :content-type :json
             :query-params query-params}))))

(defn get-issue [issue-id]
  (get-url (str "/rest/api/latest/issue/" issue-id)))

(defn issue-url [issue-id]
  (str base-url "/browse/" issue-id))

(defn format-issue [issue]
  (str (issue-url (issue "key")) " = "
    (get-in issue ["fields" "summary"]) ", status: "
    (get-in issue ["fields" "status" "name"]) ", assignee: "
    (get-in issue ["fields" "assignee" "name"])))

(defn format-issues [& issues]
  (map issues format-issue))

(defn bot-get-issue [config msg]
  (when (config "to-me")
    (let [content (message/get-content msg)
          issue-id (second (re-matches #".*?([A-Z]+-[0-9]+).*" content))]
      (when issue-id
        (try+
          (->> issue-id
            (get-issue)
            (format-issue)
            (message/respond msg))
          (catch [:status 404] _
            (message/respond msg (str (issue-url issue-id) " does not exist."))))
        true))))

(defn bot-search [config msg]
  (let [to-me (config "to-me")
        command (message/get-content-only msg)
        matches (re-matches #"(?i)(jira\s*search)\s*(.*)" command)]
    (when (and to-me (not (nil? matches)))
      (future
        (try+
          (->>
            {"jql" (last matches)
             "maxResults" 10}
            (get-url "/rest/api/latest/search")
            (#(% "issues"))
            (format-issues)
            (message/respond msg))
          (catch [:status 400] {:keys [body]}
            (->>
              (json/parse-string body)
              (#(% "errorMessages"))
              first
              (message/respond msg))))))))

(def bot-handlers [bot-get-issue bot-search])
