(ns time-tracker.web.routes
  (:require [time-tracker.projects.routes :as projects]
            [time-tracker.timers.routes   :as timers]
            [time-tracker.users.routes    :as users]
            [time-tracker.util :as util]))

(def routes ["/" [["api/" {"projects/" projects/routes
                           "timers/"   timers/routes
                           "users/"    users/routes}]
                  [true        (fn [_] (util/error-response 404 "Not found"))]
                  ]])
