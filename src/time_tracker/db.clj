(ns time-tracker.db
  (:require [time-tracker.config :as config])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn pool []
  (let [datasource (doto (ComboPooledDataSource.)
                     (.setDriverClass "org.postgresql.Driver")
                     (.setJdbcUrl config/jdbc-uri)
                     ;; expire excess connections after 30 minutes of inactivity:
                     (.setMaxIdleTimeExcessConnections (* 30 60))
                     ;; expire connections after 3 hours of inactivity:
                     (.setMaxIdleTime (* 3 60 60)))]
    {:datasource datasource}))

(defonce pooled-db (atom nil))

(defn init-db! []
  (reset! pooled-db (pool)))

(defn connection [] @pooled-db)
