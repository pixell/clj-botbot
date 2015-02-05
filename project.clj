(defproject pixell/clj-botbot "1.0.0-SNAPSHOT"

  :description "Simple bot for Flowdock flows"
  :url "https://github.com/pixell/botbot"

  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}

  :min-lein-version "2.5.0"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/tools.trace "0.7.8"]
                 [com.rallydev/clj-flowdock "1.2.4"]
                 [slingshot "0.12.1"]]

  :main clj-botbot.core

  ; Convenient way to specify system properties during development.
  ;
  ; :jvm-opts ["-DFLOWDOCK_TOKEN=flowdock-token"
  ;            "-DJENKINS_URL=http://localhost:8080"
  ;            "-DJENKINS_USERNAME=jenkins-username"
  ;            "-DJENKINS_APITOKEN=jenkins-token"
  ;            "-DJIRA_URL=http://localhost:8081"
  ;            "-DJIRA_USERNAME=jira-username"
  ;            "-DJIRA_PASSWORD=jira-password"]

  :profiles {:uberjar {:aot :all}})
