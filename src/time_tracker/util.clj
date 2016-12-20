(ns time-tracker.util
  (:require [ring.util.response :as res]
            [clj-time.core :as time]
            [clj-time.coerce]
            [environ.core :as environ]
            [clojure.walk :as walk]))

(defn error-response
  [status msg]
  (-> (res/response {:error msg})
      (res/status status)))

(def forbidden-response
  (error-response 403 "Forbidden"))

(def disallowed-method-response
  (error-response 405 "Method not allowed"))

(def not-found-response
  (error-response 404 "Not found"))

(defn snake-case->hyphenated-kw
  "In: \"key_string\"
  Out: :key-string"
  [key-string]
  (keyword (clojure.string/replace key-string #"_" "-")))

(defn statement-success?
  [result]
  (< 0 result))

(def select-success? (comp statement-success? count))

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

(defn hyphenize
  [thing]
  (-> thing
      (name)
      (clojure.string/replace #"_" "-")
      (keyword)))

(defn transform-map
  [thing-map & args]
  (let [walk-fn (apply comp args)]
    (walk/postwalk walk-fn thing-map)))

(defn map-contains?
  "Is the map `b` completely contained in `a`?"
  [a b]
  (= a (merge a b)))
