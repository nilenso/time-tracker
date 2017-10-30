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
  [connection client]
  (first (jdbc/insert! connection "client"
                       {"name"    (:name client)
                        "address" (:address client)
                        "gstin"   (:gstin client)
                        "pan"     (:pan client)})))

(defn create-point-of-contact!
  [connection poc]
  (first (jdbc/insert! connection "point_of_contact"
                       {"name"      (:name poc)
                        "phone"     (:phone poc)
                        "email"     (:email poc)
                        "client_id" (:client-id poc)})))

(defn create-points-of-contact!
  [connection points-of-contact]
  (map #(create-point-of-contact! connection %) points-of-contact))

(defn retrieve-all
  "Retrieves a list of ALL the clients. No authorization checks."
  [connection]
  (retrieve-all-clients-query {} {:connection connection}))
