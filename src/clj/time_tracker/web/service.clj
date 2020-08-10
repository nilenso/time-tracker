(ns time-tracker.web.service
  (:require [bidi.ring :refer [make-handler]]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-body]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.defaults :refer :all]
            [time-tracker.web.routes :refer [routes]]
            [time-tracker.web.middleware :refer [wrap-validate
                                                 wrap-log-request-response
                                                 wrap-error-logging]]))

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
      (wrap-defaults api-defaults)))

