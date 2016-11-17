(ns time-tracker.timers.pubsub
  (:require [org.httpkit.server :refer [send!]]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers.db]
            [time-tracker.util :as util]
            [clojure.spec :as s]
            [time-tracker.timers.spec]
            [clojure.algo.generic.functor :refer [fmap]]))

;; Connection management -------

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

;; Utils ----------------------

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

(defn- validate
  [spec args]
  (when-not (s/valid? spec args)
    (throw (ex-info "Invalid args"
                    {:error "Invalid args"}))))

;; Commands -----

(defn start-timer-command!
  [channel google-id connection {:keys [timer-id started-time] :as args}]
  (validate :timers.pubsub/start-timer-args args)
  (if-let [{started-time :started_time duration :duration}
           (timers.db/start! connection timer-id started-time)]
    (broadcast-to! google-id
                   {:timer-id     timer-id
                    :started-time (util/to-epoch-seconds started-time)
                    :duration     duration})
    (send-error! channel "Could not start timer")))

(defn stop-timer-command!
  [channel google-id connection {:keys [timer-id stop-time] :as args}]
  (validate :timers.pubsub/stop-timer-args args)
  (if-let [{:keys [duration]} (timers.db/stop!
                               connection timer-id stop-time)]
    (broadcast-to! google-id
                   {:timer-id     timer-id
                    :started-time nil
                    :duration     duration})
    (send-error! channel "Could not stop timer")))

(defn delete-timer-command!
  [channel google-id connection {:keys [timer-id] :as args}]
  (validate :timers.pubsub/delete-timer-args args)
  (if (timers.db/delete!
       connection timer-id)
    (broadcast-to! google-id
                   {:timer-id timer-id
                    :delete?  true})
    (send-error! channel "Could not delete timer")))

(defn create-and-start-timer-command!
  [channel google-id connection {:keys [project-id started-time] :as args}]
  (validate :timers.pubsub/create-and-start-timer-args args)
  (let [{timer-id :id} (timers.db/create! connection project-id google-id)
        {started-time :started_time duration :duration}
        (timers.db/start! connection timer-id started-time)]
    (broadcast-to! google-id
                   {:timer-id     timer-id
                    :project-id   project-id
                    :started-time (util/to-epoch-seconds started-time)
                    :duration     duration
                    :create?      true})))

(defn change-timer-duration-command!
  [channel google-id connection {:keys [timer-id duration current-time] :as args}]
  (validate :timers.pubsub/change-timer-duration-args args)
  (if-let [{started-time :started_time duration :duration}
           (timers.db/update-duration! connection
                                       timer-id
                                       (timers.db/map->TimePeriod duration)
                                       current-time)]
    (broadcast-to! google-id
                   {:timer-id     timer-id
                    :started-time (util/to-epoch-seconds started-time)
                    :duration     duration})
    (send-error! channel "Could not update duration")))

;; Middleware ----

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

(defn- wrap-owns-timer
  [func]
  (fn [channel google-id connection args]
    (if-let [timer-id (:timer-id args)] ;; Necessary b/c validation hasn't happened yet
      (if (timers.db/owns? connection google-id timer-id)
        (func channel google-id connection args)
        (send-error! channel "Unauthorized"))
      (send-invalid-args! channel))))

(defn- wrap-can-create-timer
  [func]
  (fn [channel google-id connection args]
    (if-let [project-id (:project-id args)]
      (if (timers.db/has-timing-access? connection google-id project-id)
        (func channel google-id connection args)
        (send-error! channel "Unauthorized"))
      (send-invalid-args! channel))))

;; Routes --

(defn- wrap-middlewares
  "Wraps commands in a command map with the give set of middleware.
  The first middleware is the 'outermost'."
  [middlewares command-map]
  (fmap (apply comp middlewares) command-map))

(def command-map
  (wrap-middlewares
   [wrap-validator wrap-transaction]
   (merge {"create-and-start-timer" (wrap-can-create-timer
                                     create-and-start-timer-command!)}
          (wrap-middlewares
           [wrap-owns-timer]
           {"start-timer"            start-timer-command!
            "stop-timer"             stop-timer-command!
            "delete-timer"           delete-timer-command!
            "change-timer-duration"  change-timer-duration-command!}))))

;; -------

(defn dispatch-command!
  "Calls the appropriate timer command."
  [channel google-id command-data]
  (if-let [command-fn (command-map (get command-data :command))]
    (command-fn channel google-id (dissoc command-data :command))
    ;; TODO: Add error logging
    (send! channel (json/encode
                    {:error "Invalid command"}))))