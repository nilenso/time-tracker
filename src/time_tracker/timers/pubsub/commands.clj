(ns time-tracker.timers.pubsub.commands
  (:require [time-tracker.timers.db :as timers-db]
            [time-tracker.timers.pubsub.io :as io]
            [time-tracker.util :as util]))

;; Commands -----

;; A command receives `channel`, `google-id`, `connection` and `command-data` as
;; arguments. Here `connection` is a database connection.

;; Every command received from the client should have a :command field, and other
;; args as necessary.

;; Every message pushed from the server should have a :type field, and other args
;; as necessary.

(defn stop-timer-now!
  [channel google-id connection {:keys [timer-id] :as args}]
  (if-let [stopped-timer (timers-db/stop!
                          connection timer-id (util/current-epoch-seconds))]
    (io/broadcast-state-change! google-id stopped-timer :update)
    (io/send-error! channel "Could not stop timer")))

(defn start-timer-now!
  [channel google-id connection {:keys [timer-id] :as args}]
  (if-let [{:keys [started-time duration] :as started-timer}
           (timers-db/start! connection timer-id (util/current-epoch-seconds))]
    (do (io/broadcast-state-change! google-id started-timer :update)
        (let [started-timers (timers-db/retrieve-started-timers connection
                                                                google-id)
              timers-to-stop (filter #(not= (:id %) timer-id)
                                     started-timers)]
          (doseq [timer timers-to-stop]
            (stop-timer-now! channel google-id connection
                             {:timer-id  (:id timer)}))))
    (io/send-error! channel "Could not start timer")))

(defn create-and-start-timer-now!
  [channel google-id connection {:keys [project-id created-time notes] :as args}]
  (let [created-timer (timers-db/create! connection project-id google-id created-time notes)]
    (io/broadcast-state-change! google-id created-timer :create)
    (start-timer-now! channel google-id connection
                  {:timer-id     (:id created-timer)})))

(defn delete-timer!
  [channel google-id connection {:keys [timer-id] :as args}]
  (if (timers-db/delete!
       connection timer-id)
    (io/broadcast-to! google-id
                      {:type :delete
                       :id   timer-id})
    (io/send-error! channel "Could not delete timer")))

(defn update-timer!
  [channel google-id connection {:keys [timer-id duration current-time notes] :as args}]
  (if-let [updated-timer
           (timers-db/update! connection
                                       timer-id
                                       duration
                                       current-time
                                       notes)]
    (io/broadcast-state-change! google-id updated-timer :update)
    (io/send-error! channel "Could not update duration")))

(defn receive-ping!
  [channel google-id connection args]
  (io/send-data! channel {:type :pong}))
