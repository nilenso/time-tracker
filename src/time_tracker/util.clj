(ns time-tracker.util
  (:require [clj-time.core :as time]
            [clj-time.coerce]
            [environ.core :as environ]
            [clojure.walk :as walk]))


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
