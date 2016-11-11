(ns time-tracker.timers.routes
  (:require [time-tracker.timers.handlers :as handlers]
            [time-tracker.auth.core :refer [wrap-auth]]))

(def routes {"ws-connect/" (wrap-auth handlers/ws-handler)})
