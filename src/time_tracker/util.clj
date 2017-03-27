(ns time-tracker.util
  (:require [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]
            [environ.core :as environ]
            [clojure.walk :as walk]
            [clojure.spec :as s]))

(defn statement-success?
  [result]
  (< 0 result))

(def select-success? (comp statement-success? count))

(defn to-epoch-seconds
  [time-obj]
  (time-coerce/to-epoch time-obj))

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

(defn raise-validation-error [data]
  (throw (ex-info "Validation failed" (merge {:event :validation-failed}
                                             data))))

(defn validate-spec
  [value spec]
  (when-let [failure (s/explain-data spec value)]
    (raise-validation-error {:failure (pr-str failure)})))

(defn parse-int
  "Parses either a string or a keyword to an int"
  [string-or-keyword]
  (Integer/parseInt (name string-or-keyword)))

(defn bigdec-places
  "Coerces to a bigdec with `places` decimal places."
  [number places]
  (.setScale (bigdec number) places java.math.BigDecimal/ROUND_HALF_UP))

(defn round-to-two-places
  "Rounds a number to two decimal places."
  [monetary-value]
  (bigdec-places monetary-value 2))

(defn divide-money
  "Divides two bigdecs, rounding to two places.
  Rounds the result if it is recurring."
  [a b]
  (.divide a b 2 java.math.BigDecimal/ROUND_HALF_UP))

(defn eq-with-tolerance [a b tolerance]
  (< (.abs (- a b)) tolerance))

(defn utc-offset->clj-timezone
  "`utc-offset` should be in minutes."
  [utc-offset]
  (time/time-zone-for-offset (int (/ utc-offset 60))
                                 (rem utc-offset 60)))

(defn epoch->clj-time
  "`utc-offset` should be in minutes."
  [epoch utc-offset]
  (-> (clj-time.coerce/from-long (* 1000 (long epoch)))
      (time/to-time-zone (utc-offset->clj-timezone utc-offset))))
