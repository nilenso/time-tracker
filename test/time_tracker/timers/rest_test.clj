(ns time-tracker.timers.rest-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.projects.test-helpers :as projects-helpers]
            [time-tracker.test-helpers :as helpers]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)


(deftest list-all-owned-timers-test
  (let [gen-projects (projects-helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})
        url          "http://localhost:8000/api/timers/"
        timer1       (timers-db/create! (db/connection)
                                        (get gen-projects "foo")
                                        "gid1")
        timer2       (timers-db/create! (db/connection)
                                        (get gen-projects "foo")
                                        "gid1")
        timer3       (timers-db/create! (db/connection)
                                        (get gen-projects "goo")
                                        "gid2")]
    (testing "A user should only see the timers they own"
      (let [{:keys [status body]} (helpers/http-request :get url "gid1")]
        (is (= 200 status))
        (is (= (set (map :id [timer1 timer2]))
               (set (map #(get % "id") body))))))))
