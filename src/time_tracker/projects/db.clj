(ns time-tracker.projects.db
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [yesql.core :refer [defqueries]]
            [time-tracker.util :refer [statement-success?]]))

(defqueries "time_tracker/projects/sql/db.sql")

(defn retrieve-if-authorized
  "Retrieves one specific project if authorized."
  [project-id google-id]
  (first (retrieve-if-authorized-query {:google_id  google-id
                                        :project_id project-id
                                        :permission "admin"}
                                       {:connection (db/connection)})))

(defn update-if-authorized!
  "Updates a project if authorized and returns the updated project."
  [project-id {:keys [name]} google-id]
  ;; The Postgres RETURNING clause doesn't work,
  ;; so using two queries for now
  (jdbc/with-db-transaction [connection (db/connection)]
    (when (statement-success?
           (update-if-authorized-query! {:name       name
                                         :google_id  google-id
                                         :project_id project-id
                                         :permission "admin"}
                                        {:connection connection}))
      (jdbc/get-by-id connection "project" project-id))))

(defn delete-if-authorized!
  "Deletes a project and returns true if authorized."
  [project-id google-id]
  (statement-success?
   (delete-if-authorized-query! {:google_id  google-id
                                 :project_id project-id
                                 :permission "admin"}
                                {:connection (db/connection)})))

(defn retrieve-authorized-projects
  "Retrieves a (possibly empty) list of authorized projects."
  [google-id]
  (retrieve-authorized-projects-query {:google_id  google-id
                                       :permission "admin"}
                                      {:connection (db/connection)}))

(defn- admin?
  [connection google-id]
  (let [authorized-query-result (first (is-admin-query {:google_id google-id
                                                        :role      "admin"}
                                                       {:connection connection}))]
    (statement-success? (:count authorized-query-result))))

(defn create-if-authorized!
  [contents google-id]
  (jdbc/with-db-transaction [connection (db/connection)]
    (when (admin? connection google-id)
      (let [{project-id :id :as created-project}
            (first (jdbc/insert! connection "project"
                                 {"name" (:name contents)}))

            {user-id :id}
            (first (jdbc/find-by-keys connection "app_user"
                                      {"google_id" google-id}))]
        (create-admin-permission-query! {:project_id project-id
                                         :user_id    user-id}
                                        {:connection connection})
        created-project))))
