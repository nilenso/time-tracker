(ns time-tracker.util
  (:require [ring.util.response :as res]
            [clj-time.core :as time]
            [clj-time.coerce]
            [environ.core :as environ]))

(defn error-response
  [status msg]
  (-> (res/response {:error msg})
      (res/status status)))

(def forbidden-response
  (error-response 403 "Forbidden"))

(def disallowed-method-response
  (error-response 405 "Method not allowed"))

(defn snake-case->hyphenated-kw
  "In: \"key_string\"
  Out: :key-string"
  [key-string]
  (keyword (clojure.string/replace key-string #"_" "-")))

(defn statement-success?
  [result]
  (< 0 result))

(defn to-epoch-seconds
  [time-obj]
  (/ (clj-time.coerce/to-long time-obj) 1000.0))

(defn current-epoch-seconds []
  (to-epoch-seconds (time/now)))

(defn from-config
  [config-var]
  (if-let [result (environ/env config-var)]
    result
    (throw (ex-info "Config var not defined" {:var config-var}))))
