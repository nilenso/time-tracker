(ns time-tracker.projects.routes
  (:require [time-tracker.projects.handlers :as handlers]
            [time-tracker.auth.core :refer [wrap-auth]]
            [clojure.algo.generic.functor :refer [fmap]]
            [clojure.java.jdbc :as jdbc]
            [time-tracker.db :refer [wrap-transaction]]
            [time-tracker.users.core :refer [wrap-autoregister]]))

(def middleware (comp wrap-auth wrap-transaction wrap-autoregister))

(defn routes []
  {[:id "/"] (fmap middleware
                   {:get    handlers/retrieve
                    :put    handlers/modify
                    :delete handlers/delete})
   ""        (fmap middleware
                   {:get  handlers/list-all
                    :post handlers/create})})

