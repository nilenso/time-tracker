(ns time-tracker.web.routes
  (:require [time-tracker.users.routes    :as users]
            [time-tracker.projects.routes :as projects]
            [time-tracker.timers.routes   :as timers]
            [time-tracker.util :as util]))

(def routes ["/" [["users/"    users/routes]
                  ["projects/" projects/routes]
                  ["timers/"   timers/routes]
                  [true        (fn [_] (util/error-response 404 "Not found"))]
                  ]])
