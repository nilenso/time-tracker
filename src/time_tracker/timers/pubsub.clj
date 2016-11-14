(ns time-tracker.timers.pubsub
  (:require [org.httpkit.server :refer [send!]]
            [cheshire.core :as json]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers.db]
            [time-tracker.util :as util]
            [clojure.spec :as s]
            [time-tracker.timers.spec]))

;; A map of google-ids to sets of channels.
(defonce active-connections (atom {}))

(defn- conj-to-set
  "If the-set is nil, returns a new set containing value.
  Behaves like conj otherwise."
  [the-set value]
  (if (nil? the-set)
    #{value}
    (conj the-set value)))

(defn add-channel!
  "Adds a channel to the map of active connections."
  [channel google-id]
  (swap! active-connections update google-id conj-to-set channel))

(defn on-close!
  "Called when a channel is closed."
  [channel google-id status]
  ;; TODO: Use status in logging
  (swap! active-connections update google-id disj channel))

(defn broadcast-to!
  "Serializes data and sends it to all connections belonging to
  google-id."
  [google-id data]
  (let [str-data (json/encode data)]
    (doseq [channel (get @active-connections google-id)]
      (send! channel str-data))))

(defn send-error!
  [channel message]
  (send! channel (json/encode
                  {:error message})))

(defn send-invalid-args!
  [channel]
  (send-error! channel "Invalid args"))

(defn command-dispatch-fn
  [channel google-id command-data]
  (:command command-data))

(defmulti run-command! command-dispatch-fn)

(defmethod run-command! :default
  [channel _ _]
  (send! channel (json/encode
                  {:error "Invalid command"})))

(defmethod run-command! "start-timer"
  [channel google-id {:keys [timer-id started-time] :as args}]
  (if-not (s/valid? :timers.pubsub/start-timer-args args)
    (send-invalid-args! channel)
    (if-let [{started-time :started_time duration :duration}
             (timers.db/start-if-authorized! (db/connection) timer-id started-time google-id)]
      (broadcast-to! google-id
                     {:timer-id     timer-id
                      :started-time (util/to-epoch-seconds started-time)
                      :duration     duration})
      (send-error! channel "Could not start timer"))))

(defmethod run-command! "stop-timer"
  [channel google-id {:keys [timer-id stop-time] :as args}]
  (if-not (s/valid? :timers.pubsub/stop-timer-args args)
    (send-invalid-args! channel)
    (if-let [{:keys [duration]} (timers.db/stop-if-authorized!
                                 (db/connection) timer-id stop-time google-id)]
      (broadcast-to! google-id
                     {:timer-id     timer-id
                      :started-time nil
                      :duration     duration})
      (send-error! channel "Could not stop timer"))))

(defmethod run-command! "delete-timer"
  [channel google-id {:keys [timer-id] :as args}]
  (if-not (s/valid? :timers.pubsub/delete-timer-args args)
    (send-invalid-args! channel)
    (if (timers.db/delete-if-authorized!
         (db/connection) timer-id google-id)
      (broadcast-to! google-id
                     {:timer-id timer-id
                      :delete?  true})
      (send-error! channel "Could not delete timer"))))

