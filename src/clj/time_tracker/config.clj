(ns time-tracker.config
  (:require [aero.core :as aero]))

(defonce ^:private cfg (atom nil))

(defn init
  ([]
   (init (clojure.java.io/resource "config.edn")))
  ([config]
   (reset! cfg (aero/read-config (or config
                                     (clojure.java.io/resource "config.edn"))))))

(defn get-config
  ([] @cfg)
  ([key] (get @cfg key)))
