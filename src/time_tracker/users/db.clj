(ns time-tracker.users.db
  (:require [yesql.core :refer [defqueries]]
            [time-tracker.util :as util]
            [clojure.java.jdbc :as jdbc]))

(defqueries "time_tracker/users/sql/db.sql")

(defn register-user!
  "Puts a user's details into the DB. The default role is 'user'.
  Does nothing if the user is already registered."
  [connection google-id name]
  (register-user-query! {:google_id google-id
                         :name      name}
                        {:connection connection}))

(defn registered?
  "Check if a user is in the DB"
  [connection google-id]
  (util/select-success? (retrieve-user-data-query {:google_id google-id}
                                             {:connection connection})))

(defn retrieve-user-data
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
