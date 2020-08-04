(ns time-tracker.config
  (:require [aero.core :as aero]))

(defonce ^:private cfg (atom nil))

(defn init []
  (reset! cfg (aero/read-config (clojure.java.io/resource "config.edn"))))

(defn get-config
  ([] @cfg)
  ([key] (get @cfg key)))
