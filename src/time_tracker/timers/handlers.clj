(ns time-tracker.timers.handlers
  (:require [org.httpkit.server :as http-kit]
            [time-tracker.timers.pubsub :as pubsub]
            [cheshire.core :as json]))

(defn ws-handler
  [request]
  (http-kit/with-channel request channel
    (let [google-id (get-in request [:credentials :sub])]
      (pubsub/add-channel! channel google-id)
      (http-kit/on-close channel (partial pubsub/on-close! channel google-id))
      (http-kit/on-receive channel (fn [data]
                                     (pubsub/run-command! channel google-id
                                                          (json/decode data keyword)))))))
