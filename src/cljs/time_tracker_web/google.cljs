(ns time-tracker-web.google
  (:require [re-frame.core :as rf]
            [time-tracker-web.events :as events]
            [cljs.core.async :refer [go >! <!] :as async]
            [cljs.core.async.interop :refer-macros [<p!]]))

(def google-client-id "your-client-id-here")

(defn initialize-auth-client! []
  (-> js/gapi
      .-auth2
      (.init (clj->js {"client_id" google-client-id}))))

(defn load-auth-client! []
  (let [ret (async/chan)]
    (.load js/gapi "auth2" #(go (>! ret true)))
    ret))

(defn init-api! []
  (go
    (<! (load-auth-client!))
    (<p! (initialize-auth-client!))
    (rf/dispatch [::events/google-client-initialized])))

(defn- current-user []
  (-> js/gapi
      .-auth2
      .getAuthInstance
      .-currentUser
      .get))

(defn- local-basic-profile []
  (-> (current-user)
      .getBasicProfile))

(defn get-local-profile
  "Gets the signed in user's profile information from the
  GAPI client (not the token)."
  []
  (let [profile-obj (local-basic-profile)]
    {:id    (.getId profile-obj)
     :name  (.getName profile-obj)
     :email (.getEmail profile-obj)}))

(def five-minutes-in-secs (* 60 5))

(declare refresh-token)

(defn process-auth-response
  "Processes Google's successful auth response and schedules a refresh."
  [auth-response]
  (let [{:strs [expires_in id_token]} (js->clj auth-response)]
    (rf/dispatch [::events/update-id-token id_token])
    (js/setTimeout refresh-token (* 1000 (- expires_in five-minutes-in-secs)))))

(defn- refresh-token []
  (go
    (let [new-auth-response (<p! (.reloadAuthResponse (current-user)))]
      (process-auth-response new-auth-response))))
