(ns time-tracker.clients.handlers
  (:require [ring.util.response :as res]
            [time-tracker.auth.core :refer [wrap-google-authenticated]]
            [time-tracker.clients.db :as clients.db]
            [time-tracker.clients.spec :as clients-spec]
            [time-tracker.util :as util]
            [time-tracker.web.util :as web-util]))

(defn list-all
  [request connection]
  (res/response (clients.db/retrieve-all connection)))

(defn create
  [{:keys [credentials body]} connection]
  (util/validate-spec body ::clients-spec/client)
  (let [google-id (:sub credentials)]
    (if (clients.db/has-user-role? google-id connection ["admin"])
      (let [created-client     (clients.db/create! connection body)
            id                 (:id created-client)
            poc                (:points-of-contact body)
            client-coerced-poc (map #(assoc % :client-id id) poc)
            created-poc        (clients.db/create-points-of-contact!
                                connection
                                client-coerced-poc)]
        (if (and created-client
                 (not (some nil? created-poc)))
          (-> (res/response created-client)
             (res/status 201))
          web-util/error-bad-request))
      web-util/error-forbidden)))
