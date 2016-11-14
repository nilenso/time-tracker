(ns time-tracker.config
  (:require [clojure.java.io :as io]
            [ragtime.jdbc]
            [time-tracker.util :refer [from-config]]))

(def google-tokeninfo-url "https://www.googleapis.com/oauth2/v3/tokeninfo")
(def migrations-resource-dir "migrations")


(def jdbc-uri (from-config :db-connection-string))

(def db-spec {:connection-uri jdbc-uri})

;; expire excess connections after 30 minutes of inactivity:
(def cp-max-idle-time-excess-connections (* 30 60))
;; expire connections after 3 hours of inactivity:
(def cp-max-idle-time (* 3 60 60))

(def migration-config
  {:datastore  (ragtime.jdbc/sql-database db-spec)
   :migrations (ragtime.jdbc/load-resources migrations-resource-dir)})

(def client-ids [(from-config :google-client-id)])
