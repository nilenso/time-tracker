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

(defn init!
  ([]
   (init! nil))
  ([config-file]
   (config/init config-file)
   (db/init-db!)))

(defn start-server!
  ([] (start-server! (web-service/app)))
  ([app-handler]
   (log/info {:event ::server-start})
   (reset! server (httpkit/run-server app-handler
                                      {:port (config/get-config :port)}))))

(defn -main
  [& args]
  (cli/init! args)
  (init! (:config-file (cli/opts)))
  (let [opt (dissoc (cli/opts) :config-file)]
    (if (> (count opt) 1)
      (prn "too many opts passed")
      (case (ffirst opt)
        :migrate  (migrate-db)
        :rollback (rollback-db)
        :help (print (cli/help-message) "\n")
        :serve (start-server!)
        (prn "Unknown args")))))
