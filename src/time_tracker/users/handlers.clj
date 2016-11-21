(ns time-tracker.users.handlers
  (:require [ring.util.response :as res]
            [time-tracker.auth.core :refer [wrap-auth]]))

(def user-details
  (-> (fn [request]
        (res/response (:credentials request)))
      (wrap-auth)))

