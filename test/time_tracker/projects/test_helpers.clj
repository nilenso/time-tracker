(ns time-tracker.projects.test-helpers
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [yesql.core :refer [defqueries]]))

(defqueries "time_tracker/projects/sql/db.sql")

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
                                (create-admin-permission-query! {:project_id project-id
                                                                 :user_id    user-id}
                                                                {:connection conn})
                                [project-name project-id]))
                            (into {})))))
            {}
            test-data)))
