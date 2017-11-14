(ns time-tracker.clients.db-test
  (:require
   [clojure.java.jdbc :as jdbc]
   [clojure.test :refer :all]
   [time-tracker.clients.db :as clients-db]
   [time-tracker.clients.test-helpers :as helpers]
   [time-tracker.db :as db]
   [time-tracker.fixtures :as fixtures]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db)
(use-fixtures :each fixtures/isolate-db)

(deftest retrieve-test
  (let [client1           {:name    "Client1"
                           :address "High Road"
                           :gstin   "MEDONTHAVEONE!!"
                           :pan     "PAN192QWE4"}
        client2           {:name    "Client2"
                           :address "Yet another place"
                           :gstin   "JKHDF247897OPUH"
                           :pan     "JKHDF24789"}
        keys              (keys client1)
        clients           (helpers/populate-data! {"gid1" [client1]
                                                   "gid2" [client2]})
        retrieved-clients (clients-db/retrieve-all (db/connection))]
    (is (= (map #(select-keys % keys) retrieved-clients)
           [client1 client2]))))

(deftest create-test
  (let [conn           (db/connection)
        user           (helpers/create-user! conn "gid1")
        new-client     {:name    "Client1"
                        :address "High Road"
                        :gstin   "MEDONTHAVEONE!!"
                        :pan     "PAN192QWE4"}
        created-client (clients-db/create! conn new-client)]
    (is (= (dissoc created-client :id) new-client))))

(deftest update-test
  (let [conn            (db/connection)
        client          (helpers/create-client!
                         conn
                         {:name    "Client1"
                          :address "High Road"
                          :gstin   "MEDONTHAVEONE!!"
                          :pan     "PAN192QWE4"})
        expected-client (-> client
                           (assoc :id (:id client))
                           (assoc :name "Client-new1"))
        update-status   (clients-db/modify! conn expected-client)
        actual-client   (jdbc/get-by-id conn "client" (:id client))]
    (is (= expected-client actual-client))))


(deftest create-poc-test
  (let [conn        (db/connection)
        client      (helpers/create-client!
                     conn
                     {:name    "Client1"
                      :address "High Road"
                      :gstin   "MEDONTHAVEONE!!"
                      :pan     "PAN192QWE4"})
        new-poc     {:name      "Agent Smith"
                     :phone     ""
                     :email     "smith@agents.com"
                     :client_id (:id client)}
        created-poc (clients-db/create-point-of-contact! conn new-poc)]
    (is (= new-poc (dissoc created-poc :id)))))

(deftest retrieve-poc-test
  (let [conn       (db/connection)
        client     (helpers/create-client!
                    conn
                    {:name    "Client1"
                     :address "High Road"
                     :gstin   "MEDONTHAVEONE!!"
                     :pan     "PAN192QWE4"})
        poc1       (helpers/create-poc!
                    conn
                    {:name      "Agent Smith"
                     :phone     ""
                     :email     "smith@agents.com"
                     :client_id (:id client)})
        poc2       (helpers/create-poc!
                    conn
                    {:name      "Agent Smosh"
                     :phone     "+1298378912739"
                     :email     "smosh@agents.com"
                     :client_id (:id client)})
        actual-poc (clients-db/retrieve-all-points-of-contact
                    conn
                    (:id client))]
    (is (= (map  #(select-keys % (keys poc1)) actual-poc)
           [poc1 poc2]))))


(deftest update-poc-test
  (let [conn          (db/connection)
        client        (helpers/create-client!
                       conn
                       {:name    "Client1"
                        :address "High Road"
                        :gstin   "MEDONTHAVEONE!!"
                        :pan     "PAN192QWE4"})
        created-poc   (helpers/create-poc!
                       conn
                       {:name      "Agent Smith"
                        :phone     ""
                        :email     "smith@agents.com"
                        :client_id (:id client)})
        expected-poc  (-> created-poc
                         (assoc :name "Agent Smizz")
                         (assoc :email "smizz@agents.com"))
        update-status (clients-db/modify-point-of-contact! conn expected-poc)
        actual-poc    (jdbc/get-by-id conn "point_of_contact" (:id expected-poc))]
    (is (= expected-poc actual-poc))))
