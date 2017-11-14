(ns time-tracker.clients.handlers
  (:require [clojure.java.jdbc :as jdbc]
            [ring.util.response :as res]
            [time-tracker.auth.core :refer [wrap-google-authenticated]]
            [time-tracker.clients.db :as clients.db]
            [time-tracker.clients.spec :as clients-spec]
            [time-tracker.db]
            [time-tracker.util :as util]
            [time-tracker.web.util :as web-util]))

(defn list-all
  [request connection]
  (let [clients (clients.db/retrieve-all-clients-query {} {:connection connection})
        clients-with-poc (doall
                          (map
                           (fn [client]
                             (assoc client
                                    :points-of-contact
                                    (clients.db/retrieve-all-points-of-contact
                                     connection
                                     (:id client))))
                           clients))]
    (res/response clients-with-poc)))

(defn create
  [{:keys [credentials body]} connection]
  (util/validate-spec body ::clients-spec/client)
  (let [google-id (:sub credentials)]
    (if (clients.db/has-user-role? google-id connection ["admin"])
      (let [created-client     (clients.db/create! connection body)
            id                 (:id created-client)
            poc                (:points-of-contact body)
            client-coerced-poc (map #(assoc % :client_id id) poc)
            created-poc        (clients.db/create-points-of-contact!
                                connection
                                client-coerced-poc)]
        (if (and created-client
                 (not (some nil? created-poc)))
          (-> (res/response created-client)
             (res/status 201))
          web-util/error-bad-request))
      web-util/error-forbidden)))

(defn modify
  [{:keys [credentials body]} connection]
  (util/validate-spec body ::clients-spec/client)
  (let [google-id     (:sub credentials)
        poc           (:points-of-contact body)
        needs-insert? #(= "insert" (:action %))
        new-poc       (seq (filter needs-insert? poc))
        existing-poc  (seq (remove needs-insert? poc))
        body          (dissoc body :points-of-contact)]
    (if (clients.db/has-user-role? google-id connection ["admin"])
      (let [modified-client (clients.db/modify! connection body)
            created-poc     (clients.db/create-points-of-contact! connection new-poc)
            modified-poc    (clients.db/modify-points-of-contact! connection existing-poc)]
        (if (and modified-client created-poc modified-poc)
          (-> (res/response {})
             (res/status 200))
          web-util/error-bad-request))
      web-util/error-forbidden)))
