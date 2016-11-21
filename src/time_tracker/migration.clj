(ns time-tracker.migration
  (:require [ragtime.repl]
            [time-tracker.config :refer [migration-config] :as config]
            [time-tracker.logging :as log]))

(defn migrate-db []
  (try
    (let [out (with-out-str
                (ragtime.repl/migrate migration-config))]
      (log/info {:event          ::applied-migrations
                 :ragtime-output out}))
    (catch Exception ex
      (log/error ex {:event ::migration-failed}))))

(defn rollback-db []
  (try
    (let [out (with-out-str
                (ragtime.repl/rollback migration-config))]
      (log/info {:event          ::rolled-back-migration
                 :ragtime-output out}))
    (catch Exception ex
      (log/error ex {:event ::migration-rollback-failed}))))

;; These are called by `lein migrate` and `lein rollback`.

(defn lein-migrate-db []
  (log/configure-logging! "info")
  (migrate-db))

(defn lein-rollback-db []
  (log/configure-logging! "info")
  (rollback-db))
