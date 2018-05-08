(ns time-tracker.timers.routes
  (:require [time-tracker.timers.handlers :as handlers]
            [time-tracker.auth.core :refer [wrap-auth]]
            [time-tracker.db :refer [wrap-transaction]]
            [time-tracker.web.middleware :refer [with-rest-middleware]]
            [clojure.algo.generic.functor :refer [fmap]]))

(defn routes []
  {"ws-connect/" handlers/ws-handler
   ""            (with-rest-middleware {:get handlers/list-all})})
