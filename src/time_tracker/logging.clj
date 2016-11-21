(ns time-tracker.logging
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [time-tracker.util :refer [from-config]]
            [environ.core :as environ])
  (:import [org.apache.log4j Level
            Logger
            ConsoleAppender
            PatternLayout]
           org.apache.log4j.net.SocketAppender))

(defmacro trace
  [msg]
  `(log/trace (json/encode ~msg)))

(defmacro debug
  [msg]
  `(log/debug (json/encode ~msg)))

(defmacro info
  [msg]
  `(log/info (json/encode ~msg)))

(defmacro warn
  [msg]
  `(log/warn (json/encode ~msg)))

(defmacro error
  ([msg]
   `(log/error (json/encode ~msg)))

  ([throwable msg]
   `(log/error ~throwable (json/encode ~msg))))

(defmacro fatal
  ([msg]
   `(log/fatal (json/encode ~msg)))

  ([throwable msg]
   `(log/fatal ~throwable (json/encode ~msg))))

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
