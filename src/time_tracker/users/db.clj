(ns time-tracker.users.db
  (:require [yesql.core :refer [defqueries]]
            [time-tracker.util :as util]
            [clojure.java.jdbc :as jdbc]
            [time-tracker.util :refer [statement-success?]]))

(defqueries "time_tracker/users/sql/db.sql")

(defn create!
  "Puts a user's details into the DB. The default role is 'user'.
  Does nothing if the user is already registered."
  [connection google-id name]
  (when-let [created-user (create-user-query<! {:google_id google-id
                                                :name      name}
                                               {:connection  connection})]
      (util/transform-keys created-user util/hyphenize)))

(defn registered?
  "Check if a user is in the DB"
  [connection google-id]
  (util/select-success? (retrieve-user-data-query {:google_id google-id}
                                                  {:connection connection})))

(defn retrieve
  "Retrieve data of one user."
  [connection google-id]
  (first (retrieve-user-data-query {:google_id google-id}
                                   {:connection  connection
                                    :identifiers util/hyphenize})))

(defn retrieve-all
  "Retrieves all user data."
  [connection]
  (retrieve-all-users-query {} {:connection connection
                                :identifiers util/hyphenize}))

(defn has-user-role?
  [google-id connection roles]
  (let [predicate (comp statement-success? :count first)]
    (some predicate
          (map (fn [role]
                 (has-role-query {:google_id google-id
                                  :role      role}
                                 {:connection connection}))
               roles))))
