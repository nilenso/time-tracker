(ns time-tracker.users.core
  (:require [time-tracker.users.db :as users-db]
            [time-tracker.invited-users.db :as invited-users-db]
            [time-tracker.web.util :as web-util]))

(defn wrap-autoregister
  [handler]
  (fn [{:keys [credentials] :as request} connection]
    (let [google-id   (:sub credentials)
          email       (:email credentials)
          invited?    (invited-users-db/invited-email? connection email)
          registered? (users-db/registered? connection google-id)
          username    (:name credentials)]
      (cond
        registered? (handler request connection)
        invited?    (do (users-db/create! connection google-id username email)
                        (invited-users-db/mark-registered-user-query! {:email email}
                                                                      {:connection connection})
                        (handler request connection))
        :else       web-util/error-forbidden))))
