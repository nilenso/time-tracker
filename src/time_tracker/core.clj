(ns time-tracker.core
  (:require [time-tracker.logging :as log]
            [time-tracker.web.service :as web-service]
            [time-tracker.util :refer [from-config]])
  (:use org.httpkit.server))


(defn -main
  [& args]
  (web-service/init!)
  (log/info {:event ::server-start})
  (run-server web-service/app {:port (Integer/parseInt (from-config :port))}))
