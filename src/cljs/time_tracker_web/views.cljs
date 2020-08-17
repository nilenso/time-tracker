(ns time-tracker-web.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [time-tracker-web.subscriptions :as subs]
            [time-tracker-web.google :as google]))

(defn- signin-button []
  (reagent/create-class
    {:display-name        "signin-button"
     :component-did-mount (fn [_]
                            (-> js/gapi
                                .-signin2
                                (.render "google-signin-button"
                                         (clj->js {:onsuccess (fn [^js google-user]
                                                                (-> google-user
                                                                    .getAuthResponse
                                                                    (google/process-auth-response)))
                                                   :onfailure (fn []
                                                                (println "Sign-in failed!"))}))))

     :reagent-render      (fn []
                            [:div {:id "google-signin-button"}])}))

(defn signin-page []
  (if @(rf/subscribe [::subs/google-client-initialized?])
    [:div
     [:h3 "Welcome to TT! Please sign in"]
     [signin-button]]
    [:h2 "Loading..."]))

(defn landing-page []
  (let [{:keys [name email]} (google/get-local-profile)]
    [:div
     [:h3 "This is TT"]
     [:p name]
     [:p email]]))

(defn root []
  (if @(rf/subscribe [::subs/signed-in?])
    [landing-page]
    [signin-page]))
