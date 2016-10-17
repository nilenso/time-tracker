(ns time-tracker.projects.test-helpers
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]))

(defn populate-data!
  "In: {google-id [list of owned projects]}
  Out: {project-name project-id} 
  Fills out the database with the given test project data."
  [test-data]
  (jdbc/with-db-transaction [conn (db/connection)]
    (reduce (fn [project-name-ids [google-id project-names]]
              (let [{user-id :id} (first (jdbc/insert! conn "app_user"
                                                       {"google_id" google-id
                                                        "name" "Agent Smith"}))]
                (merge project-name-ids
                       (->> (for [project-name project-names]
                              (let [{project-id :id} (first (jdbc/insert! conn "project"
                                                                          {"name" project-name}))]
                                (jdbc/execute! conn
                                               [(str "INSERT INTO project_permission "
                                                     "(project_id, app_user_id, permissions) "
                                                     "VALUES (?, ?, ARRAY['admin']::permission[]);")
                                                project-id user-id]
                                               {:multi? false})
                                [project-name project-id]))
                            (into {})))))
            {}
            test-data)))
