(ns time-tracker.config
  (:require [clojure.java.io :as io]
            [ragtime.jdbc]
            [time-tracker.util :refer [from-config]]
            [environ.core :as environ])
  (:import [org.apache.log4j Level
            Logger
            ConsoleAppender
            PatternLayout]
           org.apache.log4j.net.SocketAppender))

(def google-tokeninfo-url "https://www.googleapis.com/oauth2/v3/tokeninfo")
(def migrations-resource-dir "migrations")


(def jdbc-uri (from-config :db-connection-string))

(def db-spec {:connection-uri jdbc-uri})

;; expire excess connections after 30 minutes of inactivity:
(def cp-max-idle-time-excess-connections (* 30 60))
;; expire connections after 3 hours of inactivity:
(def cp-max-idle-time (* 3 60 60))

(def migration-config
  {:datastore  (ragtime.jdbc/sql-database db-spec)
   :migrations (ragtime.jdbc/load-resources migrations-resource-dir)})

(def client-ids [(from-config :google-client-id)])



(def log-levels {"all"   Level/ALL
                 "trace" Level/TRACE
                 "debug" Level/DEBUG
                 "info"  Level/INFO
                 "warn"  Level/WARN
                 "error" Level/ERROR
                 "fatal" Level/FATAL
                 "off"   Level/OFF})

(defn- set-logger-level*
  [^org.apache.log4j.Logger logger level]
  {:pre [(log-levels level)]}
  (.setLevel logger (log-levels level)))

(defn set-root-logger-level!
  "Sets the root logger to be at `level`."
  [level]
  (set-logger-level* (Logger/getRootLogger)
                     level))

(defn set-logger-level!
  "Sets the specified `logger` to be at `level`."
  [logger level]
  (set-logger-level* (Logger/getLogger logger)
                     level))

(defn add-root-logger-appender!
  [appender]
  (.addAppender (Logger/getRootLogger) appender))

(defn configure-logging!
  ([]
   (configure-logging! (from-config :app-log-level)))
  ([level]
   (set-root-logger-level! "error")
   (set-logger-level! "time-tracker" level)
   (add-root-logger-appender! (ConsoleAppender. (PatternLayout. "%-5p %c: %m%n")))
   (if-let [logstash-host (environ/env :logstash-host)]
     (if-let [logstash-port (environ/env :logstash-port)]
       (add-root-logger-appender! (SocketAppender. logstash-host
                                                   (Integer/parseInt logstash-port)))))))

