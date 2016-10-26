(ns time-tracker.util
  (:require [ring.util.response :as res]))

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
