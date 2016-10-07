(ns time-tracker.config
  (:require [clojure.java.io :as io]
            [nomad :refer [defconfig]]
            [ragtime.jdbc]))

(defconfig app-config (io/file "config/backend-config.edn"))

(def google-tokeninfo-url (:google-tokeninfo-url (app-config)))

(def jdbc-uri (:db-connection-string (app-config)))

(def migration-config
  {:datastore  (ragtime.jdbc/sql-database {:connection-uri jdbc-uri})
   :migrations (ragtime.jdbc/load-resources (:migrations-resource-dir (app-config)))})

(def client-ids (:client-ids (app-config)))
