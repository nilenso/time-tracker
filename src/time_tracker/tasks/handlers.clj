(ns time-tracker.tasks.handlers
  (:require [clojure.java.jdbc :as jdbc]
            [ring.util.response :as res]
            [time-tracker.auth.core :refer [wrap-google-authenticated]]
            [time-tracker.tasks.db :as tasks.db]
            [time-tracker.db]
            [time-tracker.util :as util]
            [time-tracker.web.util :as web-util]))

(defn list-all
  [request connection]
  (let [tasks (tasks.db/retrieve-all-query {} {:connection connection})]
    (res/response tasks)))
