(ns time-tracker.invited-users.db
  (:require [yesql.core :refer [defqueries]]
            [time-tracker.util :as util]
            [clojure.java.jdbc :as jdbc]))

(defqueries "time_tracker/invited_users/sql/db.sql")

(defn create!
  [connection email invited-by]
  (create-invited-user-query<! {:email email
                                :invited_by invited-by}
                               {:connection connection}))

(defn invited-email?
  [connection email]
  (= email (-> (retrieve-invited-user-query {:email email}
                                            {:connection connection})
               first
               :email)))
