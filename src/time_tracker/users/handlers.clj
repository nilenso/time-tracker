(ns time-tracker.users.handlers
  (:require [ring.util.response :as res]
            [time-tracker.users.db :as users-db]
            [time-tracker.invited-users.db :as invited-users-db]
            [time-tracker.web.util :as web-util]))

;; /users/me/

(defn retrieve
  [{:keys [credentials]} connection]
  (if-let [user-profile (users-db/retrieve connection
                                           (:sub credentials))]
    (res/response user-profile)
    web-util/error-not-found))

;; /users/

(defn list-all
  [{:keys [credentials] :as request} connection]
  (let [google-id (:sub credentials)]
    (if (users-db/has-user-role? google-id connection ["admin"])
      (res/response (users-db/retrieve-all connection))
      web-util/error-forbidden)))
