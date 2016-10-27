(ns time-tracker.migration
  (:require [ragtime.repl]
            [time-tracker.config :refer [migration-config]]))

(defn migrate-db []
  (ragtime.repl/migrate migration-config))

(defn rollback-db []
  (ragtime.repl/rollback migration-config))
