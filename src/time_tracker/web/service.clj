(ns time-tracker.web.service
  (:require [time-tracker.logging :as log]
            [bidi.ring :refer [make-handler]]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-body]]
            [ring.middleware.defaults :refer :all]
            [time-tracker.web.routes :refer [routes]]
            [time-tracker.db :as db]
            [time-tracker.util :as util]
            [cheshire.generate :refer [add-encoder encode-str]])
  (:import org.httpkit.server.AsyncChannel))

(def handler (make-handler routes))

;; This is neccessary because http-kit adds some AsyncChannel
;; to the request map if it is a websockets connection upgrade,
;; which can't be JSON serialized.
(def standard-ring-request-keys
  [:server-port :server-name :remote-addr :uri :query-string :scheme
   :headers :request-method :body])

;; Define a custom JSON encoder for the AsyncChannel
(add-encoder AsyncChannel encode-str)

(defn- wrap-log-request-response
  [handler]
  (fn [request]
    (let [response (handler request)]
      (log/debug {:event    ::request-response
                  :request  (select-keys request
                                         standard-ring-request-keys)
                  :response response})
      response)))

(defn- wrap-error-logging
  [handler]
  (fn [request]
    (try
      (if-let [response (handler request)]
        response
        (do
          (log/error {:event   ::nil-response
                      :request request})
          (util/error-response 500 "Internal Server Error")))
      (catch Exception ex
        (log/error ex {:event   ::unhandled-exception
                       :request request})
        (util/error-response 500 "Internal Server Error")))))

(def app
  (-> handler
      (wrap-log-request-response)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-defaults api-defaults)
      (wrap-error-logging)))

(defn init! []
  (log/configure-logging!)
  (db/init-db!))
