(ns time-tracker.invoices.core
  (:require [clojure.data.csv :as csv]
            [time-tracker.timers.core :as timers-core]
            [clojure.algo.generic.functor :refer [fmap]]
            [time-tracker.util :as util]))

(defn- seconds->hours
  [seconds]
  (double (/ seconds 3600)))

(defn- round-to-places
  "Rounds a floating point number to `places` decimal places."
  [number places]
  (.setScale (bigdec number) places java.math.BigDecimal/ROUND_HALF_UP))

(defn add-hours
  "Adds the hours logged against the timer entry
  to the `user-id->hours` map."
  [user-id->hours {:keys [app-user-id] :as timer}]
  (let [hours-to-add  (-> (timers-core/elapsed-time timer)
                          (seconds->hours)
                          (round-to-places 4))
        current-hours (get user-id->hours app-user-id)
        new-hours     (+ (or current-hours 0) hours-to-add)]
    (assoc user-id->hours app-user-id new-hours)))

(defn build-user-id->hours
  "Returns a map of {user-id hours-logged}"
  [required-user-ids timers]
  (let [user-id->zero (zipmap required-user-ids (repeat 0))]
    (reduce add-hours user-id->zero (vals timers))))

(defn id->name [normalized-entities]
  (fmap :name normalized-entities))

(defn csv-rows
  [user-id->hours user-id->name]
  (into [] (util/transform-keys user-id->hours user-id->name)))

(defn generate-csv
  "Generates a CSV given `users` and `timers`.
  There will be a row for every user in `users`."
  [users timers]
  (let [user-id->name  (id->name users)
        user-id->hours (build-user-id->hours (keys users) timers)]
    (with-out-str
      (csv/write-csv *out*
                     (->> (csv-rows user-id->hours user-id->name)
                          (cons ["Name" "Hours Logged"]))))))
