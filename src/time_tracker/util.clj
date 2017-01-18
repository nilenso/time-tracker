(ns time-tracker.util
  (:require [clj-time.core :as time]
            [clj-time.coerce]
            [environ.core :as environ]
            [clojure.walk :as walk]
            [clojure.spec :as s]))


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
  (clj-time.coerce/to-epoch time-obj))

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

(defn rebind-var!
  "Rebinds `v` to `new-value`, and also returns
  a function to reset the var back to its old value."
  [v new-value]
  (let [old-value (deref v)]
    (alter-var-root v (constantly new-value))
    (fn []
      (alter-var-root v (constantly old-value)))))

(defn normalize-entities
  ([coll] (normalize-entities coll :id))
  ([coll key-fn]
   (zipmap (map key-fn coll)
           coll)))

(defn transform-keys
  [m transform-fn]
  (into {}
        (for [[k v] m] [(transform-fn k) v])))

(defn validate-spec
  [value spec]
  (when-not (s/valid? spec value)
    (throw (ex-info "Validation failed" {:event :validation-failed
                                         :spec  spec
                                         :value value}))))
