(ns time-tracker.web.middleware
  (:require [clojure.algo.generic.functor :refer [fmap]]
            [time-tracker.auth.core :refer [wrap-auth]]
            [time-tracker.db :refer [wrap-transaction]]
            [time-tracker.users.core :refer [wrap-autoregister]]))

(def rest-middleware
  (comp wrap-auth wrap-transaction wrap-autoregister))

(defn with-rest-middleware
  [routes-map]
  (fmap rest-middleware routes-map))
