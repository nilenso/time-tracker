(ns time-tracker.core
  (:gen-class)
  (:require [bidi.ring :refer [make-handler]]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-body]]
            [ring.middleware.defaults :refer :all]
            
            [time-tracker.routes :refer [routes]])
  (:use org.httpkit.server))

(def handler (make-handler routes))

(def app
  (-> handler
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-defaults api-defaults)))

(defn -main
  [& args]
  (println "Starting server")
  (run-server app {:port 8000}))
