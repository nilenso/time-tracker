(ns time-tracker.users.db-test
  (:require [clojure.test :refer :all]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.db :as db]
            [time-tracker.users.db :as users-db]
            [time-tracker.users.test-helpers :as users-test-helpers]
            [clojure.spec :as s]
            [time-tracker.users.spec :as users-spec]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)

(deftest create-test
  (let [created-user (users-db/create! (db/connection) "gid1" "sandy")
        second-call  (users-db/create! (db/connection) "gid1" "sandy")]
    (is (s/valid? ::users-spec/user created-user))
    (is (nil? second-call))))

(deftest retrieve-all-test
  (users-test-helpers/create-users! ["sandy" "gid1" "user"]
                                    ["sandy2" "gid2" "user"]
                                    ["sandy3" "gid3" "user"])
  (let [users (users-db/retrieve-all (db/connection))]
    (testing "All users in the database should be retrieved"
      (is (= #{"gid1" "gid2" "gid3"}
             (set (map :google-id users)))))
    (testing "All fields should be retrieved"
      (doseq [user users]
        (is (= #{:id :google-id :name :role}
               (set (keys user))))))))
