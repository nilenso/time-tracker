(ns time-tracker.clients.test-helpers
  (:require
   [clojure.java.jdbc :as jdbc]
   [time-tracker.db :as db]))


(defn create-user!
  [connection google-id]
  ;; Not using time-tracker.users.test-helpers.create-users!
  ;; because that doesn't return the user id.
  (:id (first
        (jdbc/insert! connection "app_user"
                      {"google_id" google-id
                       "name" "Agent Smith"}))))

(defn create-client! [connection client]
  (first (jdbc/insert! connection "client" client)))

(defn create-poc! [connection poc]
  (first (jdbc/insert! connection "point_of_contact" poc)))

(defn- populate-clients!
  [connection google-id clients]
  (->> (for [client clients]
         (let [client-name (:name client)
               client-id   (:id (create-client! connection client))]
           [client-name client-id]))
       (into {})))

(defn populate-data! [test-data]
  (jdbc/with-db-transaction [conn (db/connection)]
    (let [create-clients (fn [client-ids [google-id clients]]
                           (let [user-id (create-user! conn google-id)]
                             (merge client-ids
                                    (populate-clients! conn google-id clients))))]
      (reduce create-clients {} test-data))))
