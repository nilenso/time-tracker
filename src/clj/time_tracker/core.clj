(ns time-tracker.core
  (:gen-class)
  (:require [time-tracker.migration :refer [migrate-db rollback-db]]
            [time-tracker.cli :as cli]
            [time-tracker.config :as config]
            [time-tracker.db :as db]
            [time-tracker.web.service :as web-service]))

(defn init!
  [config-file]
  (config/init config-file)
  (db/init-db!))

(defn -main
  [& args]
  (let [{:keys [config-file] :as opts} (cli/parse args)]
    (init! config-file)
    (if-let [opts-error (cli/error-message opts)]
      (do
        (prn opts-error)
        (System/exit 1))
      (case (cli/operational-mode opts)
        :migrate  (migrate-db)
        :rollback (rollback-db)
        :help (print (cli/help-message))
        :serve (web-service/start-server!)
        (prn "Unknown args")))))
