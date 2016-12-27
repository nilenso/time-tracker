(ns time-tracker.timers.handlers
  (:require [org.httpkit.server :as http-kit]
            [ring.util.response :as res]
            [time-tracker.timers.pubsub :as pubsub]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.logging :as log]
            [cheshire.core :as json]
            [time-tracker.util :as util]
            [time-tracker.timers.pubsub.state :as pubsub-state]
            [time-tracker.timers.core :as timers-core]
            [time-tracker.timers.spec]
            [time-tracker.web.util :as web-util]))

;; List endpoint ------------------------------------------------------------
;; /timers/

(defn- list-owned-timers-on-date
  [connection google-id date]
  (let [list-of-timers (timers-db/retrieve-authorized-timers
                        connection
                        google-id)]
    (res/response (filter #(timers-core/created-on? % date)
                          list-of-timers))))

(defn- list-all-owned-timers
  [connection google-id]
  (res/response (timers-db/retrieve-authorized-timers
                        connection
                        google-id)))

(defn list-all
  "When called with no payload, returns all of a user's timers.
  When called with {:date `epoch`}, returns all of the timers 
  created on the same day as `epoch`."
  [request connection]
  (web-util/validate-request request :timers.handlers/list-all-args)
  (let [google-id (get-in request [:credentials :sub])]
    (if-let [{:keys [date]} (:body request)]
      (list-owned-timers-on-date connection google-id date)
      (list-all-owned-timers connection google-id))))

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
                           (pubsub-state/remove-channel! channel)))
      (http-kit/on-receive channel (fn [data]
                                     (pubsub/dispatch-command! channel
                                                               (json/decode data keyword)))))))
