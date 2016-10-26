(ns time-tracker.timers.db
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.util :refer [statement-success?]]
            [yesql.core :refer [defqueries]]
            ;; For protocol extensions
            [clj-time.jdbc]))

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
  [project-id google-id]
  (jdbc/with-db-transaction [connection (db/connection)]
    (when (has-timing-access? connection google-id project-id)
      (create-timer-query<! {:google_id  google-id
                             :project_id project-id}
                            {:connection connection}))))

(defn update-if-authorized!
  "Set the elapsed duration of the timer."
  [timer-id duration google-id])

(defn delete-if-authorized!
  "Deletes a timer and returns true if authorized."
  [timer-id google-id])

(defn retrieve-authorized-timers
  "Retrieves all timers the user is authorized to modify."
  [google-id])

(defn start-if-authorized!
  "Starts a timer if authorized and if the timer is not already started.
  Returns {:keys [start-time duration]} or nil."
  [timer-id google-id])

(defn stop-if-authorized!
  "Stops a timer if authorized and if the timer is not already stopped.
  Returns {:keys [duration]} or nil."
  [timer-id google-id])
