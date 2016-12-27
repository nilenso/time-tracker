(ns time-tracker.web.middleware
  (:require [clojure.algo.generic.functor :refer [fmap]]
            [time-tracker.auth.core :refer [wrap-auth]]
            [time-tracker.db :refer [wrap-transaction]]
            [time-tracker.users.core :refer [wrap-autoregister]]
            [time-tracker.util :as util]
            [time-tracker.logging :as log]))

(def rest-middleware
  (comp wrap-auth wrap-transaction wrap-autoregister))

(defn with-rest-middleware
  [routes-map]
  (fmap rest-middleware routes-map))

(defn wrap-validate
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception ex
        (if (= :validation-failed
               (:event (ex-data ex)))
          (do (log/info (assoc (ex-data ex) :event ::validation-failed))
              util/bad-request-response)
          (throw ex))))))
