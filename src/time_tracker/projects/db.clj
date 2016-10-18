(ns time-tracker.projects.db
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]))

(defn retrieve-if-authorized
  "Retrieves one specific project if authorized."
  [project-id google-id]
  (let [query (str "SELECT project.* FROM project "
                   "INNER JOIN project_permission ON project.id = project_permission.project_id "
                   "INNER JOIN app_user ON app_user.id = project_permission.app_user_id "
                   "WHERE app_user.google_id = ? "
                   "AND ?::permission = ANY (project_permission.permissions) "
                   "AND project.id = ?;")
        connection (db/connection)]
    (first (jdbc/query connection [query google-id "admin" project-id]))))

(defn update-if-authorized!
  "Updates a project if authorized and returns the updated project."
  [project-id contents google-id]
  (let [update-query (str "UPDATE project "
                          "SET name = ? "
                          "FROM project_permission, app_user "
                          "WHERE project_permission.project_id = project.id "
                          "AND project_permission.app_user_id = app_user.id "
                          "AND ?::permission = ANY (project_permission.permissions) "
                          "AND app_user.google_id = ? "
                          "AND project.id = ?;")]
    ;; The Postgres RETURNING clause doesn't work,
    ;; so using two queries for now
    (jdbc/with-db-transaction [connection (db/connection)]
      (if (< 0 (first
                (jdbc/execute! connection [update-query (:name contents)
                                           "admin" google-id project-id])))
        (jdbc/get-by-id connection "project" project-id)))))

(defn delete-if-authorized!
  "Deletes a project and returns true if authorized."
  [project-id google-id]
  (let [query (str "DELETE FROM project "
                   "USING project_permission, app_user "
                   "WHERE project_permission.project_id = project.id "
                   "AND project_permission.app_user_id = app_user.id "
                   "AND ?::permission = ANY (project_permission.permissions) "
                   "AND app_user.google_id = ? "
                   "AND project.id = ?;")
        connection (db/connection)]
    (< 0 (first
          (jdbc/execute! connection [query "admin" google-id project-id])))))

(defn retrieve-authorized-projects
  "Retrieves a (possibly empty) list of authorized projects."
  [google-id]
  (let [query (str "SELECT project.* FROM project "
                   "INNER JOIN project_permission ON project.id = project_permission.project_id "
                   "INNER JOIN app_user ON app_user.id = project_permission.app_user_id "
                   "WHERE app_user.google_id = ? "
                   "AND ?::permission = ANY (project_permission.permissions);")]
    (jdbc/query (db/connection) [query google-id "admin"])))

(defn- admin?
  [connection google-id]
  (let [authorized-query (str "SELECT COUNT(*) FROM app_user "
                              "WHERE google_id = ? "
                              "AND role = ?::user_role")
        authorized-query-result (first (jdbc/query connection [authorized-query google-id "admin"]))]
    (< 0 (:count authorized-query-result))))

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
        (jdbc/execute! connection
                       [(str "INSERT INTO project_permission "
                             "(project_id, app_user_id, permissions) "
                             "VALUES (?, ?, ARRAY['admin']::permission[]);")
                        project-id user-id])
        created-project))))
