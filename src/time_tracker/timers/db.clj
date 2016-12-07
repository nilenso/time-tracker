(ns time-tracker.timers.db
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.util :refer [statement-success?] :as util]
            [yesql.core :refer [defqueries]]
            ;; For protocol extensions
            [clj-time.jdbc]))

(defqueries "time_tracker/timers/sql/db.sql")

(defn has-timing-access?
  [connection google-id project-id]
  (let [authorized-query-result (first (has-timing-access-query {:google_id  google-id
                                                                 :permission "admin"
                                                                 :project_id project-id}
                                                                {:connection connection}))]
    (statement-success? (:count authorized-query-result))))

(defn owns?
  "True if a user owns a timer."
  [connection google-id timer-id]
  (let [owns-query-result (first (owns-query {:google_id google-id
                                              :timer_id  timer-id}
                                             {:connection connection}))]
    (statement-success? (:count owns-query-result))))

(defn create!
  "Creates and returns a timer if authorized."
  [connection project-id google-id]
  (create-timer-query<! {:google_id  google-id
                         :project_id project-id}
                        {:connection connection}))

(defn update-duration!
  "Set the elapsed duration of the timer."
  [connection timer-id duration current-time]
  (when (statement-success? (update-timer-duration-query! {:duration     duration
                                                           :timer_id     timer-id
                                                           :current_time current-time}
                                                          {:connection connection}))
    (-> (retrieve-timer-query {:timer_id timer-id}
                              {:connection connection})
        (first)
        (select-keys [:started_time :duration]))))

(defn delete!
  "Deletes a timer. Returns false if the timer doesn't exist."
  [connection timer-id]
  (statement-success?
   (delete-timer-query! {:timer_id  timer-id}
                        {:connection connection})))

(defn retrieve-authorized-timers
  "Retrieves all timers the user is authorized to modify."
  [connection google-id]
  (->> (retrieve-authorized-timers-query {:google_id google-id}
                                         {:connection connection})
       (map #(select-keys % [:id :project_id :started_time :duration :time_created]))))

(defn start!
  "Starts a timer if the timer is not already started.
  Returns {:keys [start_time duration]} or nil."
  [connection timer-id current-time]
  (when (statement-success? (start-timer-query! {:timer_id     timer-id
                                                 :current_time current-time}
                                                {:connection connection}))
    (-> (retrieve-timer-query {:timer_id  timer-id}
                              {:connection connection})
        (first)
        (select-keys [:started_time :duration]))))

(defn stop!
  "Stops a timer if the timer is not already stopped.
  Returns {:keys [duration]} or nil."
  [connection timer-id current-time]
  (let [{:keys [duration] :as timer} (first (retrieve-timer-query {:timer_id timer-id}
                                                                  {:connection connection}))]
    ;; When the timer is started
    (when (:started_time timer)
      (let [started-time (util/to-epoch-seconds (:started_time timer))
            new-duration (+ duration (- current-time started-time))]
        (assert (>= new-duration duration) "Cannot stop timer before its start time")
        (when (statement-success? (stop-timer-query! {:timer_id     timer-id
                                                      :current_time current-time
                                                      :duration     new-duration}
                                                     {:connection connection}))
          (-> (retrieve-timer-query {:timer_id  timer-id}
                                    {:connection connection})
              (first)
              (select-keys [:duration])))))))

