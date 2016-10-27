(ns time-tracker.projects.routes
  (:require [time-tracker.projects.handlers :as handlers]
            [time-tracker.auth.core :refer [wrap-auth]]
            [clojure.algo.generic.functor :refer [fmap]]))

(def routes {[:id "/"] (fmap wrap-auth
                             {:get    handlers/retrieve
                              :put    handlers/modify
                              :delete handlers/delete})
             ""        (fmap wrap-auth
                             {:get  handlers/list-all
                              :post handlers/create})})

