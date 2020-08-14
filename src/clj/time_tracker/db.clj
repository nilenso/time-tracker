(ns time-tracker.db
  (:require [time-tracker.config :as config]
            [clojure.java.jdbc :as jdbc]
            [mount.core :refer [defstate]])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn- pool []
  (let [datasource
        (doto (ComboPooledDataSource.)
          (.setDriverClass "org.postgresql.Driver")
          (.setJdbcUrl (config/get-config :db-connection-string))
          (.setMaxIdleTimeExcessConnections
           (config/get-config :cp-max-idle-time-excess-connections))
          (.setMaxIdleTime
           (config/get-config :cp-max-idle-time)))]
    {:datasource datasource}))

(defstate pooled-db
  :start (pool)
  :stop (.close (:datasource pooled-db)))

(defn connection [] pooled-db)

(defn wrap-transaction
  [handler]
  (fn [request]
    (jdbc/with-db-transaction [conn (connection)]
      (handler request conn))))
