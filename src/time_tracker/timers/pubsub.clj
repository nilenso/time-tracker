(ns time-tracker.timers.pubsub
  (:require [org.httpkit.server :refer [send!] :as httpkit]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers.db]
            [time-tracker.util :as util]
            [clojure.spec :as s]
            [time-tracker.timers.spec]
            [clojure.algo.generic.functor :refer [fmap]]
            [time-tracker.logging :as log]
            [time-tracker.auth.core :refer [token->credentials]]))

;; Connection management -------

;; A map of google-ids to sets of channels.
(defonce active-connections (atom {}))

;; A map of channels to google IDs
(defonce channel->google-id (atom {}))

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
  [channel status]
  ;; See https://github.com/http-kit/http-kit/blob/protocol-api/src/org/httpkit/server.clj#L61
  ;; for the possible values of status
  (if-let [google-id (get @channel->google-id channel)]
    (do
      (swap! active-connections update google-id disj channel)
      (swap! channel->google-id dissoc channel))))

;; Utils ----------------------

(defn send-data!
  "Wrapper of send! that logs data and encodes JSON."
  [channel data]
  ;; TODO: Somehow log the recepient host.
  (log/debug (merge {:event     ::sent-data
                     :google-id (get @channel->google-id channel)}
                    data))
  (send! channel (json/encode data)))

(defn broadcast-to!
  "Serializes data and sends it to all connections belonging to
  google-id."
  [google-id data]
  (log/debug (merge {:event     ::broadcasted-data
                     :google-id google-id}
                    data))
  (let [str-data (json/encode data)]
    (doseq [channel (get @active-connections google-id)]
      (send! channel str-data))))

(defn broadcast-state-change!
  "Broadcasts the change in state of a timer."
  [google-id timer change-type]
  (broadcast-to! google-id
                 (assoc timer :type change-type)))

(defn send-error!
  [channel message]
  (send-data! channel
              {:error message}))

(defn send-invalid-args!
  [channel]
  (send-error! channel "Invalid args"))



;; Commands -----

;; A command receives `channel`, `google-id`, `connection` and `command-data` as
;; arguments. Here `connection` is a database connection.

;; Every command received from the client should have a :command field, and other
;; args as necessary.

;; Every message pushed from the server should have a :type field, and other args
;; as necessary.

(defn stop-timer-command!
  [channel google-id connection {:keys [timer-id stop-time] :as args}]
  (if-let [stopped-timer (timers.db/stop!
                          connection timer-id stop-time)]
    (broadcast-state-change! google-id stopped-timer :update)
    (send-error! channel "Could not stop timer")))

(defn start-timer-command!
  [channel google-id connection {:keys [timer-id started-time] :as args}]
  (if-let [{:keys [started-time duration] :as started-timer}
           (timers.db/start! connection timer-id started-time)]
    (do (broadcast-state-change! google-id started-timer :update)
        (let [started-timers (timers.db/retrieve-started-timers connection
                                                                google-id)
              timers-to-stop (filter #(not= (:id %) timer-id)
                                     started-timers)]
          (doseq [timer timers-to-stop]
            (stop-timer-command! channel google-id connection
                                 {:timer-id  (:id timer)
                                  :stop-time started-time}))))
    (send-error! channel "Could not start timer")))

(defn create-and-start-timer-command!
  [channel google-id connection {:keys [project-id started-time] :as args}]
  (let [created-timer (timers.db/create! connection project-id google-id)]
    (broadcast-state-change! google-id created-timer :create)
    (start-timer-command! channel google-id connection
                          {:timer-id     (:id created-timer)
                           :started-time started-time})))

(defn delete-timer-command!
  [channel google-id connection {:keys [timer-id] :as args}]
  (if (timers.db/delete!
       connection timer-id)
    (broadcast-to! google-id
                   {:type :delete
                    :id   timer-id})
    (send-error! channel "Could not delete timer")))

(defn change-timer-duration-command!
  [channel google-id connection {:keys [timer-id duration current-time] :as args}]
  (if-let [updated-timer
           (timers.db/update-duration! connection
                                       timer-id
                                       duration
                                       current-time)]
    (broadcast-state-change! google-id updated-timer :update)
    (send-error! channel "Could not update duration")))

(defn receive-ping-command!
  [channel connection google-id args]
  (send! channel (json/encode {:type :pong})))

;; Middleware ----

(defn- wrap-exception
  [func]
  (fn [channel google-id args]
    (try
      (func channel google-id args)
      (catch Exception ex
        (log/error ex {:event     ::message-handler-failed
                       :google-id google-id
                       :args      args})))))

(defn- wrap-validator
  [func spec]
  (fn [channel google-id connection args]
    (if (s/valid? spec args)
      (func channel google-id connection args)
      (send-invalid-args! channel))))

(defn- wrap-transaction
  [func]
  (fn [channel google-id args]
    (jdbc/with-db-transaction [connection (db/connection)]
      (func channel google-id connection args))))

(defn- wrap-owns-timer
  [func]
  (fn [channel google-id connection {:keys [timer-id] :as args}]
    (if (timers.db/owns? connection google-id timer-id)
      (func channel google-id connection args)
      (do
        (log/warn {:event     ::unauthorized-timer-action
                   :google-id google-id
                   :timer-id  timer-id})
        (send-error! channel "Unauthorized")))))

(defn- wrap-can-create-timer
  [func]
  (fn [channel google-id connection {:keys [project-id] :as args}]
    (if (timers.db/has-timing-access? connection google-id project-id)
      (func channel google-id connection args)
      (do
        (log/warn {:event      ::unauthorized-timer-creation
                   :google-id  google-id
                   :project-id project-id})
        (send-error! channel "Unauthorized")))))

;; Routes --

(defn- wrap-middlewares
  "Wraps commands in a command map with the give set of middleware.
  The first middleware is the 'outermost'."
  [middlewares command-map]
  (fmap (apply comp middlewares) command-map))

;; For now, anyone can log time against any project.
(def command-map
  (wrap-middlewares
   [wrap-exception wrap-transaction]
   (merge {"create-and-start-timer" (-> create-and-start-timer-command!
                                        ;;(wrap-can-create-timer)
                                        (wrap-validator
                                         :timers.pubsub/create-and-start-timer-args))}

          {"start-timer"            (-> start-timer-command!
                                        (wrap-owns-timer)
                                        (wrap-validator :timers.pubsub/start-timer-args))
           "stop-timer"             (-> stop-timer-command!
                                        (wrap-owns-timer)
                                        (wrap-validator :timers.pubsub/stop-timer-args))
           "delete-timer"           (-> delete-timer-command!
                                        (wrap-owns-timer)
                                        (wrap-validator :timers.pubsub/delete-timer-args))
           "change-timer-duration"  (-> change-timer-duration-command!
                                        (wrap-owns-timer)
                                        (wrap-validator
                                         :timers.pubsub/change-timer-duration-args))
           "ping"                   receive-ping-command!})))

;; -------

(defn authenticate-channel!
  [channel {:keys [command token]}]
  (if (= command "authenticate")
    (if-let [{google-id :sub} (token->credentials
                               [(util/from-config :google-client-id)]
                               token)]
      (do
        (swap! channel->google-id assoc channel google-id)
        (add-channel! channel google-id)
        (send! channel (json/encode {:auth-status "success"}))
        true))))

(defn dispatch-command!
  "Calls the appropriate timer command."
  [channel command-data]
  (log/debug (merge {:event ::received-data} command-data))
  (if-let [google-id (get @channel->google-id channel)]
    (if-let [command-fn (command-map (get command-data :command))]
      (command-fn channel google-id (dissoc command-data :command))
      (send! channel (json/encode
                      {:error "Invalid command"})))
    (when-not (authenticate-channel! channel command-data)
      (send! channel (json/encode
                      {:error "Authentication failure"}))
      (log/info {:event ::authentication-failure})
      (httpkit/close channel))))
