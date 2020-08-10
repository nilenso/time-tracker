(ns dev.repl-utils
  (:require [time-tracker.core :as core]
            [time-tracker.util :as util]))

;; Functions for easily starting, stopping and restarting the server in the REPL during development.

(defonce server-stop-fn (atom nil))

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
        stop-server-fn (core/start-server!)]
    (fn []
      (stop-server-fn)
      (reset-auth-var-fn))))


(defn repl-start-server! []
  (let [stop-fn (core/start-server!)]
    (reset! server-stop-fn stop-fn)))

(defn repl-stop-server! []
  (when @server-stop-fn
    (@server-stop-fn)
    (reset! server-stop-fn nil)))

(defn repl-restart-server! []
  (repl-stop-server!)
  (repl-start-server!))
