(ns time-tracker.core
  (:gen-class)
  (:require [org.httpkit.server :as httpkit]
            [time-tracker.migration :refer [migrate-db rollback-db]]
            [time-tracker.db :as db]
            [time-tracker.logging :as log]
            [time-tracker.util :as util]
            [time-tracker.web.service :as web-service]))

(defn init! []
  (log/configure-logging!)
  (db/init-db!))

(defn teardown! []
  (log/teardown-logging!))

(defn start-server!
  ([port] (start-server! (web-service/app) port))
  ([app-handler port]
   (log/info {:event ::server-start})
   (let [stop-fn (httpkit/run-server app-handler
                                     {:port (Integer/parseInt port)})]
     (fn []
       (stop-fn)
       (log/info {:event ::server-stop})
       (teardown!)))))

(defn -main
  [& args]
  (init!)
  (condp = (first args)
    "migrate"  (migrate-db)
    "rollback" (rollback-db)
    (start-server! (or (first args) (util/from-config :port)))))