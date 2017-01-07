(ns time-tracker.web.service
  (:require [time-tracker.logging :as log]
            [bidi.ring :refer [make-handler]]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-body]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.defaults :refer :all]
            [time-tracker.web.routes :refer [routes]]
            [time-tracker.web.middleware :refer [wrap-validate
                                                 wrap-log-request-response
                                                 wrap-error-logging]]
            [time-tracker.db :as db]
            [org.httpkit.server :as httpkit]
            [time-tracker.util :as util]))

(defn handler []
  (make-handler (routes)))

(defn app []
  (-> (handler)
      (wrap-validate)
      (wrap-log-request-response)
      (wrap-error-logging)
      (wrap-json-body {:keywords? true})
      (wrap-json-response)
      (wrap-params)
      (wrap-defaults api-defaults)))

(defn init! []
  (log/configure-logging!)
  (db/init-db!))

(defn teardown! []
  (log/teardown-logging!))

(defn start-server! []
  (init!)
  (log/info {:event ::server-start})
  (let [stop-fn
        (httpkit/run-server (app) {:port (Integer/parseInt (util/from-config :port))})]
    (fn []
      (stop-fn)
      (log/info {:event ::server-stop})
      (teardown!))))

(defn start-server-as-gid!
  "Starts the server and ensures that requests are always
  authenticated as `google-id`. Useful for repl testing.
  Returns a fuction to reset the var and stop the server."
  [google-id name]
  (let [reset-auth-var-fn (util/rebind-var!
                           (var time-tracker.auth.core/auth-credentials)
                           (constantly {:sub  google-id
                                        :name name
                                        :hd   "nilenso.com"}))
        stop-server-fn    (start-server!)]
    (fn []
      (stop-server-fn)
      (reset-auth-var-fn))))
