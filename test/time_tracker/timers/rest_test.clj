(ns time-tracker.timers.rest-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.projects.test-helpers :as projects-helpers]
            [time-tracker.test-helpers :as helpers]
            [time-tracker.util :as util]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)


(deftest list-all-owned-timers-test
  (let [gen-projects   (projects-helpers/populate-data! {"gid1" ["foo"]
                                                         "gid2" ["goo"]})
        url            "http://localhost:8000/api/timers/"
        current-time   (util/current-epoch-seconds)
        seconds-in-day (* 60 60 24)
        timer1         (timers-db/create! (db/connection)
                                          (get gen-projects "foo")
                                          "gid1"
                                          current-time)
        timer2         (timers-db/create! (db/connection)
                                          (get gen-projects "foo")
                                          "gid1"
                                          current-time)
        timer3         (timers-db/create! (db/connection)
                                          (get gen-projects "goo")
                                          "gid2"
                                          current-time)
        ;; Create a timer yesterday.
        timer4         (timers-db/create! (db/connection)
                                          (get gen-projects "foo")
                                          "gid1"
                                          (- current-time seconds-in-day))
        ;; Create a timer in the future.
        timer5         (timers-db/create! (db/connection)
                                          (get gen-projects "foo")
                                          "gid1"
                                          (+ current-time seconds-in-day))]
    (testing "A user should only see the timers they own"
      (let [{:keys [status body]} (helpers/http-request :get url "gid1")]
        (is (= 200 status))
        (is (= (set (map :id [timer1 timer2 timer4 timer5]))
               (set (map #(get % "id") body))))))

    (testing "A user should be able to filter timers to a particular day."
      (let [{:keys [status body]} (helpers/http-request :get url "gid1" {:date current-time})]
        (is (= 200 status))
        (is (= (set (map :id [timer1 timer2]))
               (set (map #(get % "id") body))))))

    (testing "An invalid request should fail."
      (let [{:keys [status body]} (helpers/http-request :get url "gid1" {:date "foobar"})]
        (is (= 400 status))))))
