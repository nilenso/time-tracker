(ns time-tracker.tasks.db
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]))

(defn create!
  [connection {:keys [name project-id] :as contents}]
  (first (jdbc/insert! connection
                       "task"
                       {:name name
                        :project_id project-id})))

#_(defn )
