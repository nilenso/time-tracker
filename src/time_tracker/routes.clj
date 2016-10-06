(ns time-tracker.routes
  (:require [time-tracker.users.routes :as users]))

(def routes ["/" {"users/" users/routes}])
