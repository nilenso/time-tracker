(ns time-tracker.invoices.routes
  (:require [time-tracker.web.middleware :refer [with-rest-middleware]]
            [time-tracker.invoices.handlers :as handlers]))

(defn routes []
  {"create/"       (with-rest-middleware {:post handlers/create-invoice})
   "invoice/"      (with-rest-middleware {:post handlers/generate-invoice})})
