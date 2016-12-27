(ns time-tracker.web.middleware
  (:require [clojure.algo.generic.functor :refer [fmap]]
            [time-tracker.auth.core :refer [wrap-auth]]
            [time-tracker.db :refer [wrap-transaction]]
            [time-tracker.users.core :refer [wrap-autoregister]]
            [time-tracker.web.util :as web-util]
            [time-tracker.logging :as log]
            [cheshire.generate :refer [add-encoder encode-str]])
    (:import org.httpkit.server.AsyncChannel))

(def rest-middleware
  (comp wrap-auth wrap-transaction wrap-autoregister))

(defn with-rest-middleware
  [routes-map]
  (fmap rest-middleware routes-map))

;; This is neccessary because http-kit adds some AsyncChannel
;; to the request map if it is a websockets connection upgrade,
;; which can't be JSON serialized.
(def standard-ring-request-keys
  [:server-port :server-name :remote-addr :uri :query-string :scheme
   :headers :request-method :body :params])

;; Define a custom JSON encoder for the AsyncChannel
(add-encoder AsyncChannel encode-str)

(defn wrap-log-request-response
  [handler]
  (fn [request]
    (let [response (handler request)]
      (log/debug {:event    ::request-response
                  :request  (select-keys request
                                         standard-ring-request-keys)
                  :response response})
      response)))

(defn wrap-error-logging
  [handler]
  (fn [request]
    (try
      (if-let [response (handler request)]
        response
        (do
          (log/error {:event   ::nil-response
                      :request request})
          web-util/error-internal-server-error))
      (catch Exception ex
        (log/error ex {:event   ::unhandled-exception
                       :request request})
        web-util/error-internal-server-error))))


(defn wrap-validate
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception ex
        (if (= :validation-failed
               (:event (ex-data ex)))
          (do (log/info (merge (ex-data ex)
                               {:event   ::validation-failed
                                :request request}))
              web-util/error-bad-request)
          (throw ex))))))
