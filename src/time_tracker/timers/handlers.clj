(ns time-tracker.timers.handlers
  (:require [org.httpkit.server :as http-kit]
            [time-tracker.timers.pubsub :as pubsub]
            [time-tracker.logging :as log]
            [cheshire.core :as json]))

(defn ws-handler
  [request connection]
  (http-kit/with-channel request channel
    (let [google-id      (get-in request [:credentials :sub])
          user-agent     (get-in request [:headers "user-agent"])
          remote-address (:remote-addr request)]
      (log/info {:event          ::websockets-connection-established
                 :user-agent     user-agent
                 :remote-address remote-address
                 :google-id      google-id})
      (pubsub/add-channel! channel google-id)

      ;; See https://github.com/http-kit/http-kit/blob/protocol-api/src/org/httpkit/server.clj#L61
      ;; for the possible values of status
      (http-kit/on-close channel
                         (fn [status]
                           (log/info {:event          ::websockets-connection-closed
                                      :user-agent     user-agent
                                      :remote-address remote-address
                                      :google-id      google-id
                                      :status         status})
                           (pubsub/on-close! channel google-id status)))
      (http-kit/on-receive channel (fn [data]
                                     (pubsub/dispatch-command! channel google-id
                                                               (json/decode data keyword)))))))
