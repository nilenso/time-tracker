(ns time-tracker.timers.pubsub
  (:require [org.httpkit.server :refer [send!]]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
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
  [channel google-id connection command-data]
  (:command command-data))

(defn- validate
  [spec args]
  (when-not (s/valid? spec args)
    (throw (ex-info "Invalid args"
                    {:error "Invalid args"}))))

(defmulti run-command! command-dispatch-fn)

(defn- wrap-validator
  [func]
  (fn [channel google-id args]
    (try
      (func channel google-id args)
      (catch Exception e
        (if-let [{:keys [error]} (ex-data e)]
          ;; TODO: Add logging
          (send-invalid-args! channel)
          (throw e))))))

(defn- wrap-transaction
  [func]
  (fn [channel google-id args]
    (jdbc/with-db-transaction [connection (db/connection)]
      (func channel google-id connection args))))

(def dispatch-command!
  (-> run-command!
      (wrap-transaction)
      (wrap-validator)))

(defmethod run-command! :default
  [channel _ _ _]
  (send! channel (json/encode
                  {:error "Invalid command"})))

(defmethod run-command! "start-timer"
  [channel google-id connection {:keys [timer-id started-time] :as args}]
  (validate :timers.pubsub/start-timer-args args)
  (if-let [{started-time :started_time duration :duration}
           (timers.db/start-if-authorized! connection timer-id started-time google-id)]
    (broadcast-to! google-id
                   {:timer-id     timer-id
                    :started-time (util/to-epoch-seconds started-time)
                    :duration     duration})
    (send-error! channel "Could not start timer")))
 
(defmethod run-command! "stop-timer"
  [channel google-id connection {:keys [timer-id stop-time] :as args}]
  (validate :timers.pubsub/stop-timer-args args)
  (if-let [{:keys [duration]} (timers.db/stop-if-authorized!
                               connection timer-id stop-time google-id)]
    (broadcast-to! google-id
                   {:timer-id     timer-id
                    :started-time nil
                    :duration     duration})
    (send-error! channel "Could not stop timer")))

(defmethod run-command! "delete-timer"
  [channel google-id connection {:keys [timer-id] :as args}]
  (validate :timers.pubsub/delete-timer-args args)
  (if (timers.db/delete-if-authorized!
       connection timer-id google-id)
    (broadcast-to! google-id
                   {:timer-id timer-id
                    :delete?  true})
    (send-error! channel "Could not delete timer")))

