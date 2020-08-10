(ns time-tracker.projects.test-helpers
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [yesql.core :refer [defqueries]]
            [time-tracker.projects.db :as projects-db]
            [time-tracker.clients.test-helpers :as clients.helpers]))

(defqueries "time_tracker/projects/sql/db.sql")

(defn- create-project-as-user!
  "Creates a project and gives the user admin permissions.
  Returns the project id."
  [connection google-id project-name client-id]
  (let [{project-id :id} (projects-db/create!
                          connection
                          {:name project-name
                           :client-id client-id})]
    (projects-db/grant-permission! connection google-id project-id "admin")
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
  [connection google-id project-names client-id]
  (->> (for [project-name project-names]
         (let [project-id (create-project-as-user!
                           connection google-id project-name client-id)]
           [project-name project-id]))
       (into {})))

(defn populate-data!
  "In: [{google-id [list of owned projects]} client-id]
  Out: {project-name project-id}
  Fills out the database with the given test project data."
  ([test-data]
   (let [client-id (:id (clients.helpers/create-client! (db/connection) {:name "FooClient"}))]
     (populate-data! test-data client-id)))
  ([test-data client-id]
   (jdbc/with-db-transaction [conn (db/connection)]
     (reduce (fn [project-name-ids [google-id project-names]]
               (let [user-id (create-user! conn google-id)]
                 (merge project-name-ids
                        (populate-projects! conn google-id project-names client-id))))
             {}
             test-data))))
