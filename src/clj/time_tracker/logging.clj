(ns time-tracker.logging
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as json]))

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
