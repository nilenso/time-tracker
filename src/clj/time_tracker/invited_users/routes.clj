(ns time-tracker.invited-users.routes
  (:require [time-tracker.invited-users.handlers :as handlers]
            [time-tracker.web.middleware :refer [with-rest-middleware]]))

(defn routes []
  {""        (with-rest-middleware
               {:get  handlers/list-all
                :post handlers/create})})
