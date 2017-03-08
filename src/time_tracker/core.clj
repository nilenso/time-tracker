(ns time-tracker.core
  (:gen-class)
  (:require [time-tracker.web.service :as web-service]
            [time-tracker.db :as db]
            [time-tracker.logging :as log]
            [org.httpkit.server :as httpkit]
            [time-tracker.util :as util]))

(defn init! []
  (log/configure-logging!)
  (db/init-db!))

(defn teardown! []
  (log/teardown-logging!))

(defn start-server!
  ([] (start-server! (web-service/app)))
  ([app-handler]
   (init!)
   (log/info {:event ::server-start})
   (let [stop-fn (httpkit/run-server app-handler
                                     {:port (Integer/parseInt (util/from-config :port))})]
     (fn []
       (stop-fn)
       (log/info {:event ::server-stop})
       (teardown!)))))

(defn- start-server-as-gid!
  "Starts the server and ensures that requests are always
  authenticated as `google-id`. Useful for repl testing.
  Returns a fuction to reset the var and stop the server."
  [google-id name]
  (let [reset-auth-var-fn (util/rebind-var!
                            (var time-tracker.auth.core/auth-credentials)
                            (constantly {:sub  google-id
                                         :name name
                                         :hd   "nilenso.com"}))
        stop-server-fn (start-server!)]
    (fn []
      (stop-server-fn)
      (reset-auth-var-fn))))

(defn -main
  [& args]
  (start-server!))
