(ns time-tracker.users.core-test
  (:require [clojure.test :refer :all]
            [time-tracker.users.core :as users-core]
            [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.users.db :as users-db]
            [time-tracker.invited-users.db :as invited-users-db]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db)
(use-fixtures :each fixtures/isolate-db)


(deftest wrap-autoregister-test
  (let [handler         (fn [request connection] request)
        wrapped-handler (users-core/wrap-autoregister handler)]
    (testing "User is registered"
      (let [gid "reg_gid"
            username "reg_user"
            email "reguser@reguser.com"]
        (users-db/create! (db/connection) gid username email)
        (let [fake-request {:credentials {:sub  gid
                                          :name username
                                          :email email}}
              response     (wrapped-handler fake-request (db/connection))]
          (is (= fake-request response)))))

    (testing "User is invited"
      (let [gid "inv_gid"
            username "inv_user"
            email "invuser@invuser.com"]
        (invited-users-db/create! (db/connection) email 1)
        (let [fake-request {:credentials {:sub  gid
                                          :name username
                                          :email email}}
              response     (wrapped-handler fake-request (db/connection))
              db-row       (jdbc/get-by-id (db/connection) "app_user" gid "google_id")
              invited-user-db-row (jdbc/get-by-id (db/connection) "invited_user" email "email")]
          (is (= gid (:google_id db-row)))
          (is (= username (:name db-row)))
          (is (= email (:email db-row)))

          (is (= true (:registered invited-user-db-row)))
          (is (= fake-request response)))))


    (testing "User is not invited and not registered"
      (let [gid "uninv_gid"
            username "uninv_user"
            email "uninvuser@uninvuser.com"
            fake-request {:credentials {:sub  gid
                                        :name username
                                        :email email}}
            response     (wrapped-handler fake-request (db/connection))
            db-row       (jdbc/get-by-id (db/connection) "app_user" gid "google_id")]
        (is (= db-row nil))
        (is (= 403 (:status response)))))




    #_(testing "User does not exist"
        (let [fake-request {:credentials {:sub  "gid1"
                                          :name "dondochakka"}}
              response     (wrapped-handler fake-request (db/connection))
              db-row       (jdbc/get-by-id (db/connection) "app_user" "gid1" "google_id")]
          (is (= "gid1"
                 (:google_id db-row)))
          (is (= "dondochakka"
                 (:name db-row)))
          (is (= fake-request
                 response))))
    ))
