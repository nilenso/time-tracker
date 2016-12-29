(ns time-tracker.users.handlers
  (:require [ring.util.response :as res]
            [time-tracker.users.db :as users-db]
            [time-tracker.util :as util]))

;; /users/me/

(defn retrieve
  [{:keys [credentials]} connection]
  (if-let [user-profile (users-db/retrieve-user-data connection
                                                     (:sub credentials))]
    (res/response user-profile)
    util/not-found-response))
