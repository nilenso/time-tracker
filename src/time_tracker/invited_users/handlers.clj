(ns time-tracker.invited-users.handlers
  (:require [time-tracker.invited-users.db :as invited-users-db]
            [time-tracker.users.db :as users-db]
            [time-tracker.web.util :as web-util]
            [ring.util.response :as res]))

(defn create
  [{:keys [body credentials] :as request} connection]
  (let [{:keys [email]} body
        google-id (:sub credentials)
        invited-by (users-db/retrieve connection google-id)]
    (if (users-db/has-user-role? google-id connection ["admin"])
      (let [invited-user (invited-users-db/create! connection email (:id invited-by))]
        (res/response invited-user))
      web-util/error-forbidden)))
