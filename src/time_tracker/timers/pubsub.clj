(ns time-tracker.timers.pubsub
  (:require [time-tracker.timers.pubsub.io :as io]
            [time-tracker.timers.pubsub.state :as state]
            [time-tracker.util :as util]
            [time-tracker.logging :as log]
            [time-tracker.auth.core :refer [token->credentials]]
            [time-tracker.timers.pubsub.routes :refer [command-map]]
            [org.httpkit.server :as httpkit]))

(defn authenticate-channel!
  [channel {:keys [command token]}]
  (when (= command "authenticate")
    (when-let [{google-id :sub} (token->credentials
                                 [(util/from-config :google-client-id)]
                                 token)]
      (do 
        (state/add-channel! channel google-id)
        (io/send-data! channel {:auth-status "success"})
        true))))

(defn dispatch-command!
  "Calls the appropriate timer command."
  [channel command-data]
  (log/debug (merge {:event ::received-data} command-data))
  (if-let [google-id (get @state/channel->google-id channel)]
    (if-let [command-fn (command-map (get command-data :command))]
      (command-fn channel google-id (dissoc command-data :command))
      (io/send-data! channel {:error "Invalid command"}))
    (when-not (authenticate-channel! channel command-data)
      (io/send-data! channel {:error "Authentication failure"})
      (log/info {:event ::authentication-failure})
      (httpkit/close channel))))
