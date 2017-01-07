(ns time-tracker.core
  (:gen-class)
  (:require [time-tracker.web.service :as web-service]))


(defn -main
  [& args]
  (web-service/start-server!))
