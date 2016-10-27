(ns time-tracker.users.handlers
  (:require [ring.util.response :as res]

            [time-tracker.config :as config]
            [time-tracker.auth.core :refer [wrap-google-authenticated]]))

(def user-details
  (-> (fn [request]
        (res/response (:credentials request)))
      (wrap-google-authenticated config/client-ids)))

