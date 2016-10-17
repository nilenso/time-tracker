(ns time-tracker.users.test-helpers
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]))

(defn create-users!
  "In: [name google-id role] ...
  Creates users with the given details."
  [& args]
  (jdbc/execute! (db/connection)
                 (into [(str "INSERT INTO app_user "
                             "(name, google_id, role) "
                             "VALUES (?, ?, ?::user_role);")]
                       args)
                 {:multi? true}))
