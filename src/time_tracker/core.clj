(ns time-tracker.core
  (:gen-class)
  (:require [org.httpkit.server :as httpkit]
            [time-tracker.migration :refer [migrate-db rollback-db]]
            [time-tracker.cli :as cli]
            [time-tracker.config :as config]
            [time-tracker.db :as db]
            [time-tracker.logging :as log]
            [time-tracker.web.service :as web-service]))

(defonce server (atom nil))

(defn init! [args]
  (cli/init! args)
  (config/init (:config-file (cli/opts)))
  (log/configure-logging!)
  (db/init-db!))

(defn teardown! []
  (log/teardown-logging!))

(defn start-server!
  ([] (start-server! (web-service/app)))
  ([app-handler]
   (log/info {:event ::server-start})
   (reset! server (httpkit/run-server app-handler
                                      {:port (config/get-config :port)}))))

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
  (init! args)
  (let [opt (dissoc (cli/opts) :config-file)]
    (if (> (count opt) 1)
      (prn "too many opts passed")
      (case (ffirst opt)
        :migrate  (migrate-db)
        :rollback (rollback-db)
        :help (print (cli/help-message) "\n")
        :serve (start-server!)
        (prn "Unknown args")))))
