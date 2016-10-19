(ns time-tracker.projects.test-helpers
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [yesql.core :refer [defqueries]]))

(defqueries "time_tracker/projects/sql/db.sql")

(defn- create-project-as-user!
  "Creates a project and gives the user admin permissions.
  Returns the project id."
  [connection user-id project-name]
  (let [{project-id :id} (first (jdbc/insert!
                                 connection "project"
                                 {"name" project-name}))]
    (create-admin-permission-query! {:project_id project-id
                                     :user_id    user-id}
                                    {:connection connection})
    project-id))

(defn- create-user!
  "Creates a user and returns the user id."
  [connection google-id]
  ;; Not using time-tracker.users.test-helpers.create-users!
  ;; because that doesn't return the user id.
  (:id (first
        (jdbc/insert! connection "app_user"
                      {"google_id" google-id
                       "name" "Agent Smith"}))))

(defn- populate-projects!
  "Given a user, creates projects for the user
  and grants admin permissions on those projects.
  Returns {project-name project-id}"
  [connection user-id project-names]
  (->> (for [project-name project-names]
         (let [project-id (create-project-as-user!
                           connection user-id project-name)]
           [project-name project-id]))
       (into {})))

(defn populate-data!
  "In: {google-id [list of owned projects]}
  Out: {project-name project-id} 
  Fills out the database with the given test project data."
  [test-data]
  (jdbc/with-db-transaction [conn (db/connection)]
    (reduce (fn [project-name-ids [google-id project-names]]
              (let [user-id (create-user! conn google-id)]
                (merge project-name-ids
                       (populate-projects! conn user-id project-names))))
            {}
            test-data)))
