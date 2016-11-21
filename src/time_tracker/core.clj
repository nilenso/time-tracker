(ns time-tracker.core
  (:require [time-tracker.logging :as log]
            [time-tracker.web.service :as web-service])
  (:use org.httpkit.server))


(defn -main
  [& args]
  (web-service/init!)
  (log/info {:event ::server-start})
  (run-server web-service/app {:port 8000}))
