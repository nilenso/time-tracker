(ns time-tracker.users.core
  (:require [time-tracker.users.db :as users-db]))

(defn wrap-autoregister
  [handler]
  (fn [{:keys [credentials] :as request} connection]
    (let [google-id   (:sub credentials)
          email       (:email credentials)
          registered? (users-db/registered? connection google-id)
          username    (:name credentials)]
      (cond
        registered? (handler request connection)
        :else (do (users-db/create! connection google-id username email)
                  (handler request connection))))))
