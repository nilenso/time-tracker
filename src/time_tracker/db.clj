(ns time-tracker.db
  (:require [time-tracker.config :as config]
            [clojure.java.jdbc :as jdbc])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn- pool []
  (let [datasource (doto (ComboPooledDataSource.)
                     (.setDriverClass "org.postgresql.Driver")
                     (.setJdbcUrl config/jdbc-uri)
                     (.setMaxIdleTimeExcessConnections config/cp-max-idle-time-excess-connections)
                     (.setMaxIdleTime config/cp-max-idle-time))]
    {:datasource datasource}))

(defonce ^:private pooled-db (atom nil))

(defn init-db! []
  (reset! pooled-db (pool)))

(defn connection [] @pooled-db)

(defn wrap-transaction
  [handler]
  (fn [request]
    (jdbc/with-db-transaction [conn (connection)]
      (handler request conn))))
