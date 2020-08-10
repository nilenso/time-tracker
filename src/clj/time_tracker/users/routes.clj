(ns time-tracker.users.routes
  (:require [time-tracker.users.handlers :as handlers]
            [time-tracker.web.middleware :refer [rest-middleware
                                                 with-rest-middleware]]))

(defn routes []
  {"me/" (with-rest-middleware {:get handlers/retrieve})
   ""    {:get (rest-middleware handlers/list-all)}})
