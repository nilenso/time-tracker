(ns time-tracker.projects.handlers
  (:require [ring.util.response :as res]
            [time-tracker.config :as config]
            [time-tracker.auth.core :refer [wrap-google-authenticated]]
            [time-tracker.projects.db :as projects.db]
            [time-tracker.util :as util]))

;; Single project endpoint --------------------------------------------------

(defmulti projects-single-rest-raw
  "Read, update and delete operations on a single project"
  :request-method)

(defmethod projects-single-rest-raw :default
  [request]
  util/disallowed-method-response)

(defmethod projects-single-rest-raw :get
  [{:keys [route-params credentials]}]
  (if-let [project (projects.db/retrieve-project-if-authorized
                    (Integer/parseInt (:id route-params))
                    (:sub credentials))]
    (res/response project )
    util/forbidden-response))

(defmethod projects-single-rest-raw :put
  [{:keys [route-params body credentials]}]
  ;; TODO: Validate input JSON
  (if-let [updated-project (projects.db/update-project-if-authorized!
                            (Integer/parseInt (:id route-params))
                            body
                            (:sub credentials))]
    (res/response updated-project)
    util/forbidden-response))

(defmethod projects-single-rest-raw :delete
  [{:keys [route-params credentials]}]
  (if (projects.db/delete-project-if-authorized! (Integer/parseInt (:id route-params))
                                                 (:sub credentials))
    (-> (res/response nil)
        (res/status 204))
    util/forbidden-response))

(def projects-single-rest
  (-> projects-single-rest-raw
      (wrap-google-authenticated config/client-ids)))

;; List endpoint -----------------------------------------------------------

(defmulti projects-list-rest-raw
  "List and create operations on multiple projects"
  :request-method)

(defmethod projects-list-rest-raw :default
  [request]
  util/disallowed-method-response)

(defmethod projects-list-rest-raw :get
  [{:keys [credentials]}]
  (res/response (projects.db/retrieve-authorized-projects
                 (:sub credentials))))

(defmethod projects-list-rest-raw :post
  [{:keys [credentials body]}]
  ;; TODO: Validate input JSON
  (if-let [created-project (projects.db/create-project-if-authorized!
                            body
                            (:sub credentials))]
    (-> (res/response created-project)
        (res/status 201))
    util/forbidden-response))

(def projects-list-rest
  (-> projects-list-rest-raw
      (wrap-google-authenticated config/client-ids)))
