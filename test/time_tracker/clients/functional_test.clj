(ns time-tracker.clients.functional-test
  (:require
   [clojure.string :as string]
   [clojure.test :refer :all]
   [clojure.walk]
   [time-tracker.clients.test-helpers :as client-helpers]
   [time-tracker.fixtures :as fixtures]
   [time-tracker.test-helpers :as helpers]
   [time-tracker.users.test-helpers :as user-helpers]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)

(defn client-api-url
  ([] (string/join [(helpers/settings :api-root) "clients/"]))
  ([id] (format (string/join [(helpers/settings :api-root) "clients/%s/"]) id)))

(deftest retrieve-all-clients-test
  (let [client1 {"name"    "Client1"
                 "address" "High Road"
                 "gstin"   "MEDONTHAVEONE!!"
                 "pan"     "PAN192QWE4"}
        client2 {"name"    "Client2"
                 "address" "Yet another place"
                 "gstin"   "JKHDF247897OPUH"
                 "pan"     "JKHDF24789"}
        data    (client-helpers/populate-data!
                 {"gid1" [client1 client2]})
        expected [client1 client2]]
    (testing "Any user can view clients"
      (let [{:keys [status body]} (helpers/http-request
                                   :get
                                   (client-api-url)
                                   "gid1")
            actual                (map #(select-keys % (keys client1)) body)]
        (is (= 200 status))
        (is (= expected actual))))))

(deftest create-client-test
  (user-helpers/create-users! ["Sai Abdul" "gid1" "admin"])
  (user-helpers/create-users! ["Paul Graham" "gid2" "user"])
  (let [client {"name"              "Client1"
                "address"           "High Road"
                "gstin"             "MEDONTHAVEONE!!"
                "pan"               "PAN192QWE4"
                "points-of-contact" []}]
    (testing "Only authorized admin can create client"
      (let [{:keys [status body]} (helpers/http-request
                                   :post
                                   (client-api-url)
                                   "gid1"
                                   client)
            actual                (select-keys body (keys client))]
        (is (= 201 status))
        (is (= client actual))))

    (testing "A new client cannot be created with an empty name"
      (let [{:keys [status body]} (helpers/http-request
                                   :post
                                   (client-api-url)
                                   "gid1"
                                   (assoc client :name ""))]
        (is (= 400 status))))

    (testing "An authorized non-admin user cannot create a new client"
      (let [{:keys [status body]} (helpers/http-request
                                   :post
                                   (client-api-url)
                                   "gid2"
                                   client)]
        (is (= 403 status))))

    (testing "A non-authorized admin/user cannot create a new client"
      (let [{:keys [status body]} (helpers/http-request
                                   :post
                                   (client-api-url)
                                   "gid3"
                                   client)]
        (is (= 403 status))))))

(deftest update-client-test
  (user-helpers/create-users! ["Sai Abdul" "gid1" "admin"])
  (let [client                {"name"    "Client1"
                               "address" "High Road"
                               "gstin"   "MEDONTHAVEONE!!"
                               "pan"     "PAN192QWE4"}
        data                  (client-helpers/populate-data!
                               {"gid2" [client]})]
    (testing "Only authorized admin can update a client's details"
      (let [{:keys [status body]} (helpers/http-request
                                   :put
                                   (client-api-url (get data (:name client)))
                                   "gid1"
                                   (assoc client :name "Client new"))]
        (is (= 200 status))))

    (testing "A non-admin user cannot upadte a client's details"
      (let [{:keys [status body]} (helpers/http-request
                                   :put
                                   (client-api-url (get data (:name client)))
                                   "gid2"
                                   (assoc client :name "Client new"))]
        (is (= 403 status))))

    (testing "A client cannot be updated with an empty name"
      (let [{:keys [status body]} (helpers/http-request
                                   :post
                                   (client-api-url)
                                   "gid1"
                                   (assoc client :name ""))]
        (is (= 400 status))))))
