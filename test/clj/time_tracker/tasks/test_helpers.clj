(ns time-tracker.tasks.test-helpers
  (:require  [clojure.test :as t]
             [time-tracker.tasks.db :as tasks-db]))

(defn create-task!
  [connection task-name project-id]
  (let [task-id (:id (tasks-db/create! connection {:name task-name
                                                   :project-id project-id}))]
    task-id))
