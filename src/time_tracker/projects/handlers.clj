(ns time-tracker.projects.handlers
  (:require [ring.util.response :as res]
            [time-tracker.config :as config]
            [time-tracker.auth.core :refer [wrap-google-authenticated]]
            [time-tracker.projects.db :as projects.db]
            [time-tracker.util :as util]))

;; Single project endpoint --------------------------------------------------
;; /projects/<id>/

(defn retrieve
  [{:keys [route-params credentials]}]
  (if-let [project (projects.db/retrieve-if-authorized
                    (Integer/parseInt (:id route-params))
                    (:sub credentials))]
    (res/response project )
    util/forbidden-response))

;; Calling this handler 'update' would shadow
;; clojure.core/update
(defn modify
  [{:keys [route-params body credentials]}]
  ;; TODO: Validate input JSON
  (if-let [updated-project (projects.db/update-if-authorized!
                            (Integer/parseInt (:id route-params))
                            body
                            (:sub credentials))]
    (res/response updated-project)
    util/forbidden-response))

(defn delete
  [{:keys [route-params credentials]}]
  (if (projects.db/delete-if-authorized! (Integer/parseInt (:id route-params))
                                         (:sub credentials))
    (-> (res/response nil)
        (res/status 204))
    util/forbidden-response))

;; List endpoint -----------------------------------------------------------
;; /projects/

;; list would shadow clojure.core/list
(defn list-all
  [{:keys [credentials]}]
  (res/response (projects.db/retrieve-authorized-projects
                 (:sub credentials))))

(defn create
  [{:keys [credentials body]}]
  ;; TODO: Validate input JSON
  (if-let [created-project (projects.db/create-if-authorized!
                            body
                            (:sub credentials))]
    (-> (res/response created-project)
        (res/status 201))
    util/forbidden-response))

