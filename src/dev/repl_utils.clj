(ns dev.repl-utils
  (:require [time-tracker.core :as core]))

;; Functions for easily starting, stopping and restarting the server in the REPL during development.

(defonce server-stop-fn (atom nil))

(defn repl-start-server! []
  (let [stop-fn (core/start-server!)]
    (swap! server-stop-fn (constantly stop-fn))))

(defn repl-stop-server! []
  (when @server-stop-fn
    (@server-stop-fn)
    (reset! server-stop-fn nil)))

(defn repl-restart-server! []
  (repl-stop-server!)
  (repl-start-server!))
