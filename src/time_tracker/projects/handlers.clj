(ns time-tracker.projects.handlers
  (:require [ring.util.response :as res]
            [time-tracker.auth.core :refer [wrap-google-authenticated]]
            [time-tracker.projects.db :as projects.db]
            [time-tracker.web.util :as web-util]))

;; Single project endpoint --------------------------------------------------
;; /projects/<id>/

(defn retrieve
  [{:keys [route-params credentials]} connection]
  (let [google-id  (:sub credentials)
        project-id (Integer/parseInt (:id route-params))]
    (if (projects.db/has-project-permissions?
         google-id project-id connection ["admin"])
      (if-let [project (projects.db/retrieve
                        connection
                        project-id)]
        (res/response project)
        web-util/error-not-found)
      web-util/error-forbidden)))

;; Calling this handler 'update' would shadow
;; clojure.core/update
(defn modify
  [{:keys [route-params body credentials]} connection]
  ;; TODO: Validate input JSON
  (let [google-id  (:sub credentials)
        project-id (Integer/parseInt (:id route-params))]
    (if (projects.db/has-project-permissions?
         google-id project-id connection ["admin"])
      (if-let [updated-project (projects.db/update!
                                connection
                                project-id
                                body)]
        (res/response updated-project)
        web-util/error-not-found)
      web-util/error-forbidden)))

(defn delete
  [{:keys [route-params credentials]} connection]
  (let [google-id  (:sub credentials)
        project-id (Integer/parseInt (:id route-params))]
    (if (projects.db/has-project-permissions?
         google-id project-id connection ["admin"])
      (if (projects.db/delete! connection
                               (Integer/parseInt (:id route-params)))
        (-> (res/response nil)
            (res/status 204))
        web-util/error-not-found)
      web-util/error-forbidden)))

;; List endpoint -----------------------------------------------------------
;; /projects/

;; list would shadow clojure.core/list
;; Using retrieve-all-projects instead of retrieve-authorized-projects
(defn list-all
  [request connection]
  (res/response (projects.db/retrieve-all connection)))

(defn create
  [{:keys [credentials body]} connection]
  ;; TODO: Validate input JSON
  (let [google-id (:sub credentials)]
    (if (projects.db/has-user-role?
         google-id connection ["admin"])
      (if-let [created-project (projects.db/create!
                                connection
                                body)]
        (-> (res/response created-project)
            (res/status 201))
        web-util/error-bad-request)
      web-util/error-forbidden)))

