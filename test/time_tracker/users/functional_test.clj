(ns time-tracker.users.functional-test
  (:require [clojure.test :refer :all]
            [clojure.string :as s]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.db :as db]
            [time-tracker.test-helpers :as test-helpers]
            [time-tracker.users.test-helpers :as users-helpers]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)

(defn- users-api
  []
  (s/join [(test-helpers/settings :api-root) "users/"]))

(deftest retrieve-self-user-details-test
  (let [url                   (s/join [(users-api) "me/"])
        {:keys [status body]} (test-helpers/http-request :get url "gid1")]
    (is (= 200 status))
    (is (= "gid1" (get body "google-id")))
    (is (= "user" (get body "role")))))

(deftest retrieve-all-users-test
  (users-helpers/create-users! ["sandy" "gid1" "admin" "sandy@sandy.com"]
                               ["shaaz" "gid2" "user" "shaaz@shaaz.com"])
  (testing "All the users in the database should appear in the response"
    (let [url (users-api)
          {:keys [status body]} (test-helpers/http-request :get url "gid1")]
      (is (= 200 status))
      (is (= #{{"name" "sandy", "google-id" "gid1", "role" "admin"}
               {"name" "shaaz", "google-id" "gid2", "role" "user"}}
             (->> body
                  (map #(select-keys % ["name" "google-id" "role"]))
                  (set)))))))
