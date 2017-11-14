(ns time-tracker.clients.routes
  (:require [time-tracker.clients.handlers :as handlers]
            [time-tracker.web.middleware :refer [with-rest-middleware]]))

(defn routes []
  {""        (with-rest-middleware
               {:get  handlers/list-all
                :post handlers/create})
   [:id "/"] (with-rest-middleware
               {:put handlers/modify})})
