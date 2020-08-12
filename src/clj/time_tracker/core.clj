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
  (cli/init! args)
  (init! (:config-file (cli/opts)))
  (let [opt (dissoc (cli/opts) :config-file)]
    (if (> (count opt) 1)
      (prn "too many opts passed")
      (case (ffirst opt)
        :migrate  (migrate-db)
        :rollback (rollback-db)
        :help (print (cli/help-message) "\n")
        :serve (web-service/start-server!)
        (prn "Unknown args")))))
