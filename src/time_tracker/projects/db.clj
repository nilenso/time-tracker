(ns time-tracker.projects.db
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [yesql.core :refer [defqueries]]
            [time-tracker.util :refer [statement-success?]]))

(defqueries "time_tracker/projects/sql/db.sql")

(defn has-project-permissions?
  [google-id project-id connection permissions]
  (let [predicate (comp statement-success? :count first)]
    (some predicate
          (map (fn [permission]
                 (has-project-permission-query {:google_id  google-id
                                                :project_id project-id
                                                :permission permission}
                                               {:connection connection}))
               permissions))))

(defn has-user-role?
  [google-id connection roles]
  (let [predicate (comp statement-success? :count first)]
    (some predicate
          (map (fn [role]
                 (has-role-query {:google_id google-id
                                  :role      role}
                                 {:connection connection}))
               roles))))

(defn retrieve
  "Retrieves one specific project if authorized."
  [connection project-id]
  (first (retrieve-query {:project_id project-id}
                         {:connection connection})))

(defn update!
  "Updates a project and returns the updated project."
  [connection project-id {:keys [name]}]
  ;; The Postgres RETURNING clause doesn't work,
  ;; so using two queries for now
  (when (statement-success?
         (update-query! {:name       name
                         :project_id project-id}
                        {:connection connection}))
    (jdbc/get-by-id connection "project" project-id)))

(defn delete!
  "Deletes a project and returns true."
  [connection project-id]
  (statement-success?
   (delete-query! {:project_id project-id}
                  {:connection connection})))

;; Currently not being used -- replaced with the function below
;; Will use this when authorization is enabled and figured out

(defn retrieve-authorized-projects
  "Retrieves a (possibly empty) list of authorized projects."
  [connection google-id]
  (retrieve-authorized-projects-query {:google_id  google-id
                                       :permission "admin"}
                                      {:connection connection}))

(defn retrieve-all
  "Retrieves a list of ALL the projects. No authorization checks."
  [connection]
  (retrieve-all-projects-query {} {:connection connection}))

(defn create!
  [connection contents]
  (first (jdbc/insert! connection "project"
                       {"name" (:name contents)})))

(defn grant-permission!
  "Grants a permission on a project to a user."
  [connection google-id project-id permission]
  (let [{user-id :id} (first (jdbc/find-by-keys connection "app_user"
                                                {"google_id" google-id}))]
    (statement-success?
     (grant-permission-query! {:project_id project-id
                               :user_id    user-id
                               :permission permission}
                              {:connection connection}))))
