(ns time-tracker.web.service
  (:require [bidi.ring :refer [make-handler]]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-body]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [time-tracker.web.routes :refer [routes]]
            [time-tracker.web.middleware :refer [wrap-validate
                                                 wrap-log-request-response
                                                 wrap-error-logging]]
            [time-tracker.logging :as log]
            [org.httpkit.server :as httpkit]
            [time-tracker.config :as config]))

(defn handler []
  (make-handler (routes)))

(defn app []
  (-> (handler)
      (wrap-validate)
      (wrap-log-request-response)
      (wrap-error-logging)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-params)
      (wrap-defaults api-defaults)
      (wrap-resource "public")
      (wrap-content-type)
      (wrap-not-modified)))

(defonce server (atom nil))

(defn start-server!
  ([] (start-server! (app)))
  ([app-handler]
   (log/info {:event ::server-start})
   (reset! server (httpkit/run-server app-handler
                                      {:port (config/get-config :port)}))))

(defn stop-server! []
  (@server)
  (reset! server nil))
