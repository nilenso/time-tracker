(ns time-tracker.timers.routes
  (:require [time-tracker.timers.handlers :as handlers]
            [time-tracker.auth.core :refer [wrap-auth]]
            [time-tracker.db :refer [wrap-transaction]]
            [time-tracker.users.core :refer [wrap-autoregister]]
            [clojure.algo.generic.functor :refer [fmap]]))

(def middleware (comp wrap-auth wrap-transaction wrap-autoregister))

(def routes {"ws-connect/" (middleware handlers/ws-handler)
             ""            (fmap middleware
                                 {:get handlers/list-all})})
