(ns time-tracker.users.routes
  (:require [time-tracker.users.handlers :as handlers]
            [time-tracker.web.middleware :refer [with-rest-middleware]]))

(defn routes []
  {"me/" (with-rest-middleware {:get handlers/retrieve})
   ""    (with-rest-middleware {:get handlers/list-all})})
