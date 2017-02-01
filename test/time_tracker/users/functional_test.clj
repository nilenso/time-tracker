(ns time-tracker.users.functional-test
  (:require [clojure.test :refer :all]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.db :as db]
            [time-tracker.test-helpers :as test-helpers]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)


(deftest retrieve-self-user-details-test
  (let [url                   "http://localhost:8000/api/users/me/"
        {:keys [status body]} (test-helpers/http-request :get url "gid1")]
    (is (= 200 status))
    (is (= "gid1" (get body "google-id")))
    (is (= "user" (get body "role")))))
