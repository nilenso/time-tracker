(ns time-tracker.timers.pubsub.middleware
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.logging :as log]
            [time-tracker.timers.pubsub.io :as io]
            [clojure.spec :as s]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.timers.spec]))

(defn wrap-exception
  [func]
  (fn [channel google-id args]
    (try
      (func channel google-id args)
      (catch Exception ex
        (log/error ex {:event     ::message-handler-failed
                       :google-id google-id
                       :args      args})))))

(defn wrap-validator
  [func spec]
  (fn [channel google-id connection args]
    (if (s/valid? spec args)
      (func channel google-id connection args)
      (io/send-invalid-args! channel))))

(defn wrap-transaction
  [func]
  (fn [channel google-id args]
    (jdbc/with-db-transaction [connection (db/connection)]
      (func channel google-id connection args))))

(defn wrap-owns-timer
  [func]
  (fn [channel google-id connection {:keys [timer-id] :as args}]
    (if (timers-db/owns? connection google-id timer-id)
      (func channel google-id connection args)
      (do
        (log/warn {:event     ::unauthorized-timer-action
                   :google-id google-id
                   :timer-id  timer-id})
        (io/send-error! channel "Unauthorized")))))

(defn wrap-can-create-timer
  [func]
  (fn [channel google-id connection {:keys [project-id] :as args}]
    (if (timers-db/has-timing-access? connection google-id project-id)
      (func channel google-id connection args)
      (do
        (log/warn {:event      ::unauthorized-timer-creation
                   :google-id  google-id
                   :project-id project-id})
        (io/send-error! channel "Unauthorized")))))
