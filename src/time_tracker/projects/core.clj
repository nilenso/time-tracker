(ns time-tracker.projects.core
  (:require [clojure.string :as string]))

(defn client [project]
  (-> (string/split (:name project) #"\|" 2)
      (first)))
