(ns time-tracker.config
  (:require [clojure.java.io :as io]
            [environ.core :refer [env]]
            [ragtime.jdbc]))

(def google-tokeninfo-url "https://www.googleapis.com/oauth2/v3/tokeninfo")
(def migrations-resource-dir "migrations")


(def jdbc-uri (env :db-connection-string))

(def migration-config
  {:datastore  (ragtime.jdbc/sql-database {:connection-uri jdbc-uri})
   :migrations (ragtime.jdbc/load-resources migrations-resource-dir)})

(def client-ids [(env :google-client-id)])
