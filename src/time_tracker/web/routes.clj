(ns time-tracker.web.routes
  (:require [time-tracker.clients.routes :as clients]
            [time-tracker.projects.routes :as projects]
            [time-tracker.timers.routes :as timers]
            [time-tracker.users.routes :as users]
            [time-tracker.invited-users.routes :as invited-users]
            [time-tracker.tasks.routes :as tasks]
            [time-tracker.invoices.routes :as invoices]
            [time-tracker.web.util :as web-util]))

(defn routes []
  ["/" [["api/"      {"projects/"      (projects/routes)
                      "timers/"        (timers/routes)
                      "users/"         (users/routes)
                      "invited-users/" (invited-users/routes)
                      "invoices/"      (invoices/routes)
                      "clients/"       (clients/routes)
                      "tasks/"         (tasks/routes)}]
        [true   (fn [_] web-util/error-not-found)]]])
