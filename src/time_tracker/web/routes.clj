(ns time-tracker.web.routes
  (:require [time-tracker.projects.routes :as projects]
            [time-tracker.timers.routes   :as timers]
            [time-tracker.users.routes    :as users]
            [time-tracker.web.util        :as web-util]))

(def routes ["/" [["api/" {"projects/" projects/routes
                           "timers/"   timers/routes
                           "users/"    users/routes}]
                  [true   (fn [_] web-util/error-not-found)]
                  ]])
