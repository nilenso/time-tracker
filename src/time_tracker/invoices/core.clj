(ns time-tracker.invoices.core
  (:require [clojure.data.csv :as csv]
            [time-tracker.timers.core :as timers-core]))

(defn empty-time-map
  "Returns a map of {user-id {project-id 0}}
  for all users and projects. The last entry is the number of hours.
  There must be at least one user and one project."
  [users projects]
  (->> (for [user-id (keys users)
             project-id (keys projects)]
         {user-id {project-id 0}})
       (apply merge-with merge)))

(defn- seconds->hours
  [seconds]
  (double (/ seconds 3600)))

(defn add-hours
  "Adds the hours logged against the timer entry
  to the time map."
  [time-map {:keys [project-id app-user-id] :as timer}]
  (update-in time-map
             [app-user-id project-id]
             #(+ % (seconds->hours (timers-core/elapsed-time timer)))))

(defn build-time-map
  "Returns a map of {user-id {project-id hours-logged}}.
  There must be at least one user and one project."
  [users projects timers]
  (reduce #(add-hours %1 %2)
          (empty-time-map users projects)
          (vals timers)))

(defn- round-to-places
  "Rounds a floating point number to `places` decimal places."
  [number places]
  (.setScale (bigdec number) places java.math.BigDecimal/ROUND_HALF_UP))

(defn time-map->csv-rows
  [time-map users projects timers]
  (for [[user-id project->hours] time-map
        [project-id hours]       project->hours]
    [(get-in users [user-id :name])
     (get-in projects [project-id :name])
     (round-to-places hours 4)]))

(defn generate-csv
  [users projects timers]
  (let [time-map (build-time-map users projects timers)]
    (with-out-str
      (csv/write-csv *out*
                     (-> (time-map->csv-rows
                          time-map users projects timers)
                         (conj ["Name" "Project" "Hours Logged"]))))))

