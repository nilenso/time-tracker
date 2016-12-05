(ns time-tracker.users.core-test
  (:require [clojure.test :refer :all]
            [time-tracker.users.core :as users-core]
            [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.fixtures :as fixtures]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db)
(use-fixtures :each fixtures/isolate-db)


(deftest wrap-autoregister-test
  (let [handler         (fn [request connection] request)
        wrapped-handler (users-core/wrap-autoregister handler)]
    (testing "User does not exist"
      (let [fake-request {:credentials {:sub  "gid1"
                                        :name "dondochakka"}}
            response     (wrapped-handler fake-request (db/connection))
            db-row       (jdbc/get-by-id (db/connection) "app_user" "gid1" "google_id")]
        (is (= "gid1"
               (:google_id db-row)))
        (is (= "dondochakka"
               (:name db-row)))
        (is (= fake-request
               response))))))
