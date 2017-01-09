(ns time-tracker.timers.db
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.util :refer [statement-success?] :as util]
            [clojure.algo.generic.functor :refer [fmap]]
            [yesql.core :refer [defqueries]]
            ;; For protocol extensions
            [clj-time.jdbc]))

(defqueries "time_tracker/timers/sql/db.sql")

(def timer-keys [:id :project_id :started_time :duration :time_created])

(defn- hyphenize-walk
  [thing]
  (if (keyword? thing)
    (util/hyphenize thing)
    thing))

(defn epochize
  [thing]
  (if (instance? org.joda.time.DateTime thing)
    (util/to-epoch-seconds thing)
    thing))

(defn transform-timer-map
  [timer-map]
  (-> timer-map
      (select-keys timer-keys)
      (util/transform-map hyphenize-walk epochize)))

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
  "Creates and returns a timer."
  [connection project-id google-id created-time]
  (-> (create-timer-query<! {:google_id    google-id
                             :project_id   project-id
                             :created_time created-time}
                            {:connection connection})
      (transform-timer-map)))

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
        (transform-timer-map))))

(defn delete!
  "Deletes a timer. Returns false if the timer doesn't exist."
  [connection timer-id]
  (statement-success?
   (delete-timer-query! {:timer_id  timer-id}
                        {:connection connection})))

(defn retrieve-all
  "Retrieves all timers."
  [connection]
  (retrieve-all-query {} {:connection connection
                          :identifiers util/hyphenize
                          :row-fn #(fmap epochize %)}))

(defn retrieve-authorized-timers
  "Retrieves all timers the user is authorized to modify."
  [connection google-id]
  (->> (retrieve-authorized-timers-query {:google_id google-id}
                                         {:connection connection})
       (map transform-timer-map)))

(defn retrieve-started-timers
  "Retrieves all timers which the user is authorized to modify
  and which are started."
  [connection google-id]
  (->> (retrieve-started-timers-query {:google_id google-id}
                                      {:connection connection})
       (map transform-timer-map)))

(defn start!
  "Starts a timer if the timer is not already started.
  Returns the started timer or nil."
  [connection timer-id current-time]
  (when (statement-success? (start-timer-query! {:timer_id     timer-id
                                                 :current_time current-time}
                                                {:connection connection}))
    (-> (retrieve-timer-query {:timer_id  timer-id}
                              {:connection connection})
        (first)
        (transform-timer-map))))

(defn stop!
  "Stops a timer if the timer is not already stopped.
  Returns the stopped timer or nil."
  [connection timer-id current-time]
  (let [{:keys [duration] :as timer} (first (retrieve-timer-query {:timer_id timer-id}
                                                                  {:connection connection}))]
    ;; When the timer is started
    (when (:started_time timer)
      (let [started-time (util/to-epoch-seconds (:started_time timer))
            new-duration (+ duration (- current-time started-time))]
        (when (and (>= new-duration duration)
                   (statement-success? (stop-timer-query! {:timer_id     timer-id
                                                           :current_time current-time
                                                           :duration     new-duration}
                                                          {:connection connection})))
          (-> (retrieve-timer-query {:timer_id  timer-id}
                                    {:connection connection})
              (first)
              (transform-timer-map)))))))
