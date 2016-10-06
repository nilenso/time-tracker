(ns time-tracker.users.routes
  (:require [time-tracker.users.handlers :as handlers]))

(def routes {"me/" handlers/user-details})
