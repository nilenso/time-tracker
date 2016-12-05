(ns time-tracker.users.db
  (:require [yesql.core :refer [defqueries]]
            [time-tracker.util :refer [select-success? statement-success?]]
            [clojure.java.jdbc :as jdbc]))

(defqueries "time_tracker/users/sql/db.sql")

(defn register-user!
  "Puts a user's details into the DB. The default role is 'user'. Returns the inserted row."
  [connection google-id name]
  (first (jdbc/insert! connection
                       "app_user"
                       {"google_id" google-id
                        "name"      name})))

(defn registered?
  "Check if a user is in the DB"
  [connection google-id]
  (select-success? (is-registered-query {:google_id google-id}
                                        {:connection connection})))
