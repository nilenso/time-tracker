(ns time-tracker.users.handlers
  (:require [ring.util.response :as res]
            [time-tracker.users.db :as users-db]
            [time-tracker.web.util :as web-util]))

;; /users/me/

(defn retrieve
  [{:keys [credentials]} connection]
  (if-let [user-profile (users-db/retrieve connection
                                           (:sub credentials))]
    (res/response user-profile)
    web-util/error-not-found))
