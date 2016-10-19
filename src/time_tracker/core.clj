(ns time-tracker.core
  (:require [bidi.ring :refer [make-handler]]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-body]]
            [ring.middleware.defaults :refer :all]
            [time-tracker.routes :refer [routes]]
            [time-tracker.db :as db])
  (:use org.httpkit.server))

(def handler (make-handler routes))

(def app
  (-> handler
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-defaults api-defaults)))

(defn init! []
  (db/init-db!))

(defn -main
  [& args]
  (init!)
  (println "Starting server")
  (run-server app {:port 8000}))
