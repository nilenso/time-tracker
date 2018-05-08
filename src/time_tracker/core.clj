(ns time-tracker.core
  (:gen-class)
  (:require [org.httpkit.server :as httpkit]
            [time-tracker.migration :refer [migrate-db rollback-db]]
            [time-tracker.db :as db]
            [time-tracker.logging :as log]
            [time-tracker.util :as util]
            [time-tracker.web.service :as web-service]))

(defonce server (atom nil))

(defn init! []
  (log/configure-logging!)
  (db/init-db!))

(defn teardown! []
  (log/teardown-logging!))

(defn start-server!
  ([] (start-server! (web-service/app)))
  ([app-handler]
   (log/info {:event ::server-start})
   (reset! server (httpkit/run-server app-handler
                                      {:port (Integer/parseInt (util/from-config :port))}))))

(defn stop-server!
  []
  (when-not (nil? @server)
    (@server :timeout 100)
    (log/info {:event ::server-stop})
    (reset! server nil)))

(defn restart-server!
  []
  (stop-server!)
  (start-server!))

(defn -main
  [& args]
  (init!)
  (case (first args)
    "migrate"  (migrate-db)
    "rollback" (rollback-db)
    (start-server!)))
