(ns time-tracker.clients.db
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.util :refer [statement-success?]]
            [yesql.core :refer [defqueries]]))

(defqueries "time_tracker/clients/sql/db.sql")

(defn has-user-role?
  [google-id connection roles]
  (let [predicate (comp statement-success? :count first)]
    (some predicate
          (map (fn [role]
                 (has-role-query {:google_id google-id
                                  :role      role}
                                 {:connection connection}))
               roles))))

(defn create!
  [connection {:keys [name address gstin pan]}]
  (let [row {"name"    name
             "address" address
             "gstin"   gstin
             "pan"     pan}]
    (first (jdbc/insert! connection "client" row))))

(defn modify!
  [connection client]
  (let [where-clause ["id = ?" (:id client)]]
    (first (jdbc/update! connection "client" client where-clause))))

(defn create-point-of-contact!
  [connection {:keys [name phone email client_id]}]
  (let [row {"name"      name
             "phone"     phone
             "email"     email
             "client_id" client_id}]
    (first (jdbc/insert! connection "point_of_contact" row))))

(defn create-points-of-contact!
  [connection points-of-contact]
  (doall
   (map #(create-point-of-contact! connection %)
      points-of-contact)))

(defn retrieve-all-points-of-contact
  [connection client-id]
  (retrieve-poc-query {:client_id client-id}
                      {:connection connection}))

(defn modify-point-of-contact!
  [connection poc]
  (let [where-clause ["id = ?" (:id poc)]]
    (first (jdbc/update! connection
                         "point_of_contact"
                         (-> poc
                            (dissoc :id)
                            (dissoc :client_id))
                         where-clause))))

(defn modify-points-of-contact!
  [connection pocs]
  (doall
   (map #(modify-point-of-contact! connection %)
      pocs)))

(defn retrieve-all
  "Retrieves a list of ALL the clients. No authorization checks."
  [connection]
  (retrieve-all-clients-query {} {:connection connection}))
