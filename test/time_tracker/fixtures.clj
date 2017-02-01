(ns time-tracker.fixtures
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.migration :refer [migrate-db]]
            [time-tracker.db :as db]
            [time-tracker.web.service :refer [app]]
            [time-tracker.auth.core :as auth]
            [time-tracker.auth.test-helpers :refer [fake-token->credentials]]
            [time-tracker.core :as core])
  (:use org.httpkit.server))

(defn init! [f]
  (core/init!)
  (f))

(defn destroy-db []
  (jdbc/with-db-transaction [conn (db/connection)]
    (jdbc/execute! conn "DROP SCHEMA IF EXISTS public CASCADE;")
    (jdbc/execute! conn "CREATE SCHEMA IF NOT EXISTS public;")))

(defn migrate-test-db [f]
  (migrate-db)
  (f)
  (destroy-db))

(defn serve-app [f]
  (with-redefs [auth/token->credentials
                fake-token->credentials]
    (let [stop-fn (run-server (app) {:port 8000})]
      (f)
      (stop-fn :timeout 100))))

(defn isolate-db [f]
  (jdbc/with-db-transaction [conn (db/connection)]
    (jdbc/db-set-rollback-only! conn)
    (with-redefs [db/connection (fn [] conn)]
      (f))))

