(ns time-tracker.invoices.routes
  (:require [time-tracker.web.middleware :refer [with-rest-middleware]]
            [time-tracker.invoices.handlers :as handlers]))

(defn routes []
  { ""        (with-rest-middleware
                {:post handlers/create
                 :get handlers/list-all})
   [:id "/"] (with-rest-middleware
               {:get handlers/retrieve})})
