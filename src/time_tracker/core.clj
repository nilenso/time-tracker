(ns time-tracker.core
  (:require [time-tracker.logging :as log]
            [bidi.ring :refer [make-handler]]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-body]]
            [ring.middleware.defaults :refer :all]
            [time-tracker.routes :refer [routes]]
            [time-tracker.db :as db]
            [time-tracker.config :as config]
            [time-tracker.util :as util])
  (:use org.httpkit.server))

(def handler (make-handler routes))

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
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-defaults api-defaults)
      (wrap-error-logging)))

(defn init! []
  (log/configure-logging!)
  (db/init-db!))

(defn -main
  [& args]
  (init!)
  (log/info {:event ::server-start})
  (run-server app {:port 8000}))
