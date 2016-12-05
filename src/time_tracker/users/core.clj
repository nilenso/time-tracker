(ns time-tracker.users.core
  (:require [time-tracker.users.db :as users-db]))

(defn wrap-autoregister
  [handler]
  (fn [{:keys [credentials] :as request} connection]
    (let [google-id (:sub credentials)
          name      (:name credentials)]
      (when-not (users-db/registered? connection google-id)
        (users-db/register-user! connection google-id name))
      (handler request connection))))
