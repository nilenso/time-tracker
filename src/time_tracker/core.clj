(ns time-tracker.core
  (:require [time-tracker.logging :as log]
            [bidi.ring :refer [make-handler]]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-body]]
            [ring.middleware.defaults :refer :all]
            [time-tracker.routes :refer [routes]]
            [time-tracker.db :as db]
            [time-tracker.config :as config])
  (:use org.httpkit.server))

(def handler (make-handler routes))

(def app
  (-> handler
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-defaults api-defaults)))

(defn init! []
  (config/configure-logging!)
  (db/init-db!))

(defn -main
  [& args]
  (init!)
  (log/info {:event ::server-start})
  (run-server app {:port 8000}))
