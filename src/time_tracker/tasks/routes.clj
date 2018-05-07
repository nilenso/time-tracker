(ns time-tracker.tasks.routes
  (:require [time-tracker.tasks.handlers :as handlers]
            [time-tracker.web.middleware :refer [with-rest-middleware]]))

(defn routes []
  {""        (with-rest-middleware
               {:get  handlers/list-all
                :post handlers/create})})
