(ns time-tracker.timers.handlers
  (:require [org.httpkit.server :as http-kit]
            [ring.util.response :as res]
            [time-tracker.timers.pubsub :as pubsub]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.logging :as log]
            [cheshire.core :as json]
            [time-tracker.util :as util]))

;; List endpoint ------------------------------------------------------------
;; /timers/

(defn list-all
  [request connection]
  (let [google-id      (get-in request [:credentials :sub])
        list-of-timers (timers-db/retrieve-authorized-timers
                        connection
                        google-id)]
    (res/response list-of-timers)))

(defn ws-handler
  [request]
  (http-kit/with-channel request channel
    (let [user-agent     (get-in request [:headers "user-agent"])
          remote-address (:remote-addr request)]
      (log/info {:event          ::websockets-connection-established
                 :user-agent     user-agent
                 :remote-address remote-address})

      ;; See https://github.com/http-kit/http-kit/blob/protocol-api/src/org/httpkit/server.clj#L61
      ;; for the possible values of status
      (http-kit/on-close channel
                         (fn [status]
                           (log/info {:event          ::websockets-connection-closed
                                      :user-agent     user-agent
                                      :remote-address remote-address
                                      :status         status})
                           (pubsub/on-close! channel status)))
      (http-kit/on-receive channel (fn [data]
                                     (pubsub/dispatch-command! channel
                                                               (json/decode data keyword)))))))
