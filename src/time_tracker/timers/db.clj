(ns time-tracker.timers.db
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.util :refer [statement-success?]]
            [yesql.core :refer [defqueries]]
            ;; For protocol extensions
            [clj-time.jdbc])
  (:import org.postgresql.util.PGInterval))

(defrecord TimePeriod [hours minutes seconds]
  jdbc/ISQLParameter
  (set-parameter [value statement index]
    (.setObject statement
                index
                (PGInterval. 0 0 0
                             (:hours   value)
                             (:minutes value)
                             (:seconds value)))))

(extend-protocol jdbc/IResultSetReadColumn
  PGInterval
  (result-set-read-column [^PGInterval interval _ _]
    (TimePeriod. (.getHours   interval)
                 (.getMinutes interval)
                 (.getSeconds interval))))

(defqueries "time_tracker/timers/sql/db.sql")

(defn- has-timing-access?
  [connection google-id project-id]
  (let [authorized-query-result (first (has-timing-access-query {:google_id  google-id
                                                                 :permission "admin"
                                                                 :project_id project-id}
                                                                {:connection connection}))]
    (statement-success? (:count authorized-query-result))))

(defn create-if-authorized!
  "Creates and returns a timer if authorized."
  [conn project-id google-id]
  (jdbc/with-db-transaction [connection conn]
    (when (has-timing-access? connection google-id project-id)
      (create-timer-query<! {:google_id  google-id
                             :project_id project-id}
                            {:connection connection}))))

(defn update-duration-if-authorized!
  "Set the elapsed duration of the timer."
  [conn timer-id duration current-time google-id]
  (jdbc/with-db-transaction [connection conn]
    (when (statement-success? (update-timer-duration-query! {:duration     duration
                                                             :timer_id     timer-id
                                                             :current_time current-time
                                                             :google_id    google-id}
                                                            {:connection connection}))
      (-> (retrieve-timer-query {:timer_id timer-id
                                 :google_id google-id}
                                {:connection connection})
          (first)
          (select-keys [:started_time :duration])))))

(defn delete-if-authorized!
  "Deletes a timer and returns true if authorized."
  [connection timer-id google-id]
  (statement-success?
   (delete-if-authorized-query! {:google_id google-id
                                 :timer_id  timer-id}
                                {:connection connection})))

(defn retrieve-authorized-timers
  "Retrieves all timers the user is authorized to modify."
  [connection google-id]
  (->> (retrieve-authorized-timers-query {:google_id google-id}
                                         {:connection connection})
       (map #(select-keys % [:id :project_id :started_time :duration :time_created]))))

(defn start-if-authorized!
  "Starts a timer if authorized and if the timer is not already started.
  Returns {:keys [start_time duration]} or nil."
  [conn timer-id current-time google-id]
  (jdbc/with-db-transaction [connection conn]
    (when (statement-success? (start-timer-query! {:timer_id     timer-id
                                                   :current_time current-time
                                                   :google_id    google-id}
                                                  {:connection connection}))
      (-> (retrieve-timer-query {:timer_id  timer-id
                                 :google_id google-id}
                                {:connection connection})
          (first)
          (select-keys [:started_time :duration])))))

(defn stop-if-authorized!
  "Stops a timer if authorized and if the timer is not already stopped.
  Returns {:keys [duration]} or nil."
  [conn timer-id current-time google-id]
  (jdbc/with-db-transaction [connection conn]
    (when (statement-success? (stop-timer-query! {:timer_id     timer-id
                                                  :current_time current-time
                                                  :google_id    google-id}
                                                 {:connection connection}))
      (-> (retrieve-timer-query {:timer_id  timer-id
                                 :google_id google-id}
                                {:connection connection})
          (first)
          (select-keys [:duration])))))

