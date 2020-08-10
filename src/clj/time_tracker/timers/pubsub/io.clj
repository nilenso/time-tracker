(ns time-tracker.timers.pubsub.io
  (:require [org.httpkit.server :refer [send!]]
            [cheshire.core :as json]
            [time-tracker.logging :as log]
            [time-tracker.timers.pubsub.state :as state]))

(defn send-data!
  "Wrapper of send! that logs data and encodes JSON."
  [channel data]
  ;; TODO: Somehow log the recepient host.
  (log/debug (merge {:event     ::sent-data
                     :google-id (get @state/channel->google-id channel)}
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
    (doseq [channel (state/active-connections google-id)]
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
