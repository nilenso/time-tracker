(ns time-tracker.db
  (:require [time-tracker.util :refer [from-config]]
            [clojure.java.jdbc :as jdbc])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn- pool []
  (let [datasource
        (doto (ComboPooledDataSource.)
          (.setDriverClass "org.postgresql.Driver")
          (.setJdbcUrl (from-config :db-connection-string))
          (.setMaxIdleTimeExcessConnections
           (Integer/parseInt (from-config :cp-max-idle-time-excess-connections)))
          (.setMaxIdleTime
           (Integer/parseInt (from-config :cp-max-idle-time))))]
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
