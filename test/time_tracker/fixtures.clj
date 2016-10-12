(ns time-tracker.fixtures
  (:require [clojure.java.jdbc :as jdbc]

            [time-tracker.migration :refer [migrate-db]]
            [time-tracker.db :as db]))

(defn migrate-test-db [f]
  (println "Migrating the databse")
  (migrate-db)
  (println "The database has been migrated.")
  (f))

(defn isolate-db [f]
  (jdbc/with-db-transaction [conn (db/connection)]
    (jdbc/db-set-rollback-only! conn)
    (with-redefs [db/connection (fn [] conn)]
      (f))))
