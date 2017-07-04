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
            [time-tracker.web.util :as web-util]))

(defn- list-owned-timers-between-epochs
  [connection google-id params]
  (let [{:keys [start end]} (web-util/coerce-and-validate-epoch-range params)
        list-of-timers      (timers-db/retrieve-between-authorized
                             connection google-id start end)]
    (res/response list-of-timers)))

(defn- list-all-owned-timers
  [connection google-id]
  (res/response (timers-db/retrieve-authorized-timers
                 connection
                 google-id)))

;; /api/timers/list-all/
;; /api/timers/list-all/?start=12&end=36

(defn list-all
  "When called with no payload, returns all of a user's timers.
  When called with {:keys [start end]}, returns all of the timer
  created between `start` and `end`, `end` exclusive."
  [request connection]
  (let [google-id (get-in request [:credentials :sub])]
    (if (empty? (:params request))
      (list-all-owned-timers connection google-id)
      (list-owned-timers-between-epochs connection google-id (:params request)))))

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
                                     (try
                                       (log/debug {:event ::received-data
                                                   :data  data})
                                       (pubsub/dispatch-command! channel
                                                                 (json/decode data keyword))
                                       (catch Throwable e
                                         (log/error e {:error ::websockets-error
                                                       :data data})))))
      ;; Send a "ready" message to the client to confirm
      ;; that it can send message without them getting dropped.
      ;; https://github.com/http-kit/http-kit/issues/318
      (http-kit/send! channel (json/encode {:type "ready"})))))
