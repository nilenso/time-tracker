(ns time-tracker.projects.routes
  (:require [time-tracker.projects.handlers :as handlers]
            [time-tracker.auth.core :refer [wrap-auth]]
            [clojure.algo.generic.functor :refer [fmap]]
            [clojure.java.jdbc :as jdbc]
            [time-tracker.db :refer [wrap-transaction]]
            [time-tracker.web.middleware :refer [with-rest-middleware]]))


(defn routes []
  {[:id "/"] (with-rest-middleware {:get    handlers/retrieve
                                    :put    handlers/modify
                                    :delete handlers/delete})
   ""        (with-rest-middleware {:get  handlers/list-all
                                    :post handlers/create})})

