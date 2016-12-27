(ns time-tracker.web.service
  (:require [time-tracker.logging :as log]
            [bidi.ring :refer [make-handler]]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-body]]
            [ring.middleware.defaults :refer :all]
            [time-tracker.web.routes :refer [routes]]
            [time-tracker.web.middleware :refer [wrap-validate
                                                 wrap-log-request-response
                                                 wrap-error-logging]]
            [time-tracker.db :as db]))

(def handler (make-handler routes))

(def app
  (-> handler
      (wrap-validate)
      (wrap-log-request-response)
      (wrap-error-logging)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-defaults api-defaults)))

(defn init! []
  (log/configure-logging!)
  (db/init-db!))
