(ns time-tracker.invited-users.handlers
  (:require [time-tracker.invited-users.db :as invited-users-db]
            [time-tracker.users.db :as users-db]
            [time-tracker.web.util :as web-util]
            [ring.util.response :as res]
            [time-tracker.util :refer [from-config] :as util]
            [mailgun.mail :as mail]
            [time-tracker.logging :as log]))

(defn send-email
  [email]
  (let [creds {:key (from-config :mailgun-key) :domain (from-config :mailgun-domain)}
        content {:from    "admin@time.nilenso.com"
                 :to      email
                 :subject "You have been invited to Join Time Tracker"
                 :html    "Welcome, Visit https://time.nilenso.com and Sign In with this email"}]
    (mail/send-mail creds content)
    (log/info "Email Sent Successfully Via Mail Gun")))

(defn create
  [{:keys [body credentials] :as request} connection]
  (let [{:keys [email]} body
        google-id (:sub credentials)
        invited-by (users-db/retrieve connection google-id)]
    (if (users-db/has-user-role? google-id connection ["admin"])
      (let [invited-user (invited-users-db/create! connection email (:id invited-by))]
        (send-email email)
        (res/response invited-user))
      web-util/error-forbidden)))

(defn list-all
  [{:keys [credentials]} connection]
  (let [google-id (:sub credentials)]
    (if (users-db/has-user-role? google-id connection ["admin"])
      (res/response (invited-users-db/retrieve-all connection))
      web-util/error-forbidden)))
