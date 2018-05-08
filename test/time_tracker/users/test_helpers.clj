(ns time-tracker.users.test-helpers
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]))

(defn create-users!
  "In: [name google-id role] ...
  Creates users with the given details."
  [& args]
  ;; Not using yesql here, because it doesn't look like
  ;; yesql can run batch statements.
  (jdbc/execute! (db/connection)
                 (into [(str "INSERT INTO app_user "
                             "(name, google_id, role, email) "
                             "VALUES (?, ?, ?::user_role, ?);")]
                       args)
                 {:multi? true}))
