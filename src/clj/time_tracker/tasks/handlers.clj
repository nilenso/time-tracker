(ns time-tracker.tasks.handlers
  (:require [clojure.java.jdbc :as jdbc]
            [ring.util.response :as res]
            [time-tracker.auth.core :refer [wrap-google-authenticated]]
            [time-tracker.tasks.db :as tasks.db]
            [time-tracker.db :as db]
            [time-tracker.util :as util]
            [time-tracker.web.util :as web-util]
            [time-tracker.tasks.spec :as tasks-spec]
            [time-tracker.users.db :as users.db]))

(defn list-all
  [request connection]
  (let [tasks (tasks.db/retrieve-all-query {} {:connection connection
                                               :identifiers util/hyphenize})]
    (res/response tasks)))

(defn create
  [{:keys [credentials body]} connection]
  (util/validate-spec body ::tasks-spec/task-create-input)
  (let [google-id (:sub credentials)]
    ;; TODO: Once we add the ability to "staff" users, check user has
    ;; permissions to create task in the specified project
    (if (users.db/has-user-role? google-id
                                 connection
                                 ["admin"])
      (if-let [created-task (tasks.db/create! connection body)]
        (-> (res/response created-task)
            (res/status 201))
        web-util/error-bad-request)
      web-util/error-forbidden)))
