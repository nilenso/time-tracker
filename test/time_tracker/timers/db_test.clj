(ns time-tracker.timers.db-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers.db]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.projects.test-helpers :as projects.helpers]
            [time-tracker.users.test-helpers :as users.helpers]
            [time-tracker.util :as util]
            [clj-time.jdbc]
            [clj-time.coerce :as time.coerce])
  (:import time_tracker.timers.db.TimePeriod))

(use-fixtures :once fixtures/init-db! fixtures/migrate-test-db)
(use-fixtures :each fixtures/isolate-db)

(defn- contains-map?
  "True if all key-value pairs in m2 are in m1."
  [m1 m2]
  (= m2 (select-keys m1 (keys m2))))


(deftest create-if-authorized
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})]

    (testing "Authorized project"
      (let [project-id    (get gen-projects "foo")
            created-timer (timers.db/create-if-authorized! project-id
                                                           "gid1")
            actual-timer  (first (jdbc/find-by-keys (db/connection) "timer"
                                                    {"project_id" project-id}))]
        (is (some? created-timer))
        (is (some? actual-timer))
        (is (contains-map? actual-timer created-timer))))

    (testing "Unauthorized project"
      (let [project-id    (get gen-projects "goo")
            created-timer (timers.db/create-if-authorized! project-id
                                                           "gid1")
            actual-timer  (first (jdbc/find-by-keys (db/connection) "timer"
                                                    {"project_id" project-id}))]
        (is (nil? created-timer))
        (is (nil? actual-timer))))))


(deftest update-duration-if-authorized
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})
        timer1       (timers.db/create-if-authorized! (get gen-projects "foo")
                                                      "gid1")
        timer2       (timers.db/create-if-authorized! (get gen-projects "goo")
                                                      "gid2")
        timer3       (timers.db/create-if-authorized! (get gen-projects "foo")
                                                      "gid1")]
    (testing "Owned timer"
      (testing "Not started"
        (let [timer-id        (:id timer1)
              current-time    (util/current-epoch-seconds)
              updated-project (timers.db/update-duration-if-authorized!
                               timer-id
                               (TimePeriod. 0 7 0.0)
                               current-time
                               "gid1")]
          (is (= (TimePeriod. 0 7 0.0)
                 (:duration updated-project)))
          (is (nil? (:started_time updated-project)))))

      (testing "Started"
        (let [timer-id        (:id timer3)
              start-result    (timers.db/start-if-authorized!
                               timer-id
                               (util/current-epoch-seconds)
                               "gid1")
              current-time    (util/current-epoch-seconds)
              updated-project (timers.db/update-duration-if-authorized!
                               timer-id
                               (TimePeriod. 0 9 0.0)
                               current-time
                               "gid1")]
          (is (some? start-result))
          (is (= (TimePeriod. 0 9 0.0)
                 (:duration updated-project)))
          (is (=  current-time
                  (-> (:started_time updated-project)
                      (util/to-epoch-seconds)))))))

    (testing "Unowned timer"
      (let [timer-id        (:id timer2)
            current-time    (util/current-epoch-seconds)
            updated-project (timers.db/update-duration-if-authorized!
                             timer-id
                             (TimePeriod. 0 5 0.0)
                             current-time
                             "gid1")]
        (is (nil? updated-project))))))


(defn- create-timers!
  "Creates n timers for each project and returns a set of their ids."
  [google-id gen-projects project-names n]
  (set (for [project-id (map gen-projects project-names)
             _          (range n)]
         (:id (timers.db/create-if-authorized!
               project-id
               google-id)))))


(deftest retrieve-authorized-timers
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo" "goo"]
                                                       "gid2" ["bar" "baz"]})
        expected1    (create-timers! "gid1" gen-projects ["foo" "goo"] 2)
        expected2    (create-timers! "gid2" gen-projects ["bar" "baz"] 2)
        actual1      (->> (timers.db/retrieve-authorized-timers "gid1")
                          (map :id)
                          (set))
        actual2      (->> (timers.db/retrieve-authorized-timers "gid2")
                          (map :id)
                          (set))]
    (is (= expected1 actual1))
    (is (= expected2 actual2))))


(deftest delete-if-authorized
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})
        timer1       (timers.db/create-if-authorized! (get gen-projects "foo")
                                                      "gid1")
        timer2       (timers.db/create-if-authorized! (get gen-projects "goo")
                                                      "gid2")]

    (testing "Owned timer"
      (let [timer-id     (:id timer1)
            deleted-bool (timers.db/delete-if-authorized! timer-id "gid1")
            actual-timer (jdbc/get-by-id (db/connection) "timer" timer-id)]
        (is deleted-bool)
        (is (nil? actual-timer))))

    (testing "Unowned timer"
      (let [timer-id     (:id timer2)
            deleted-bool (timers.db/delete-if-authorized! timer-id "gid1")
            actual-timer (jdbc/get-by-id (db/connection) "timer" timer-id)]
        (is (not deleted-bool))
        (is (not (nil? actual-timer)))))))


(deftest start-if-authorized
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})
        timer1       (timers.db/create-if-authorized! (get gen-projects "foo")
                                                      "gid1")
        timer2       (timers.db/create-if-authorized! (get gen-projects "goo")
                                                      "gid2")
        timer3       (timers.db/create-if-authorized! (get gen-projects "foo")
                                                      "gid1")]
    (testing "Owned timer"
      (testing "Not started"
        (let [timer-id     (:id timer1)
              current-time (util/current-epoch-seconds)
              start-result (timers.db/start-if-authorized!
                            timer-id
                            current-time
                            "gid1")]
          (is (= current-time
                 (util/to-epoch-seconds (:started_time start-result))))))
      (testing "Started"
        (let [timer-id      (:id timer2)
              first-result  (timers.db/start-if-authorized!
                             timer-id
                             (util/current-epoch-seconds)
                             "gid2")
              second-result (timers.db/start-if-authorized!
                             timer-id
                             (util/current-epoch-seconds)
                             "gid2")]
          (is (some? first-result))
          (is (nil? second-result)))))

    (testing "Unowned timer"
      (let [timer-id     (:id timer3)
            start-result (timers.db/start-if-authorized!
                          timer-id
                          (util/current-epoch-seconds)
                          "gid2")]
        (is (nil? start-result))))))


(deftest stop-if-authorized
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})
        timer1       (timers.db/create-if-authorized! (get gen-projects "foo")
                                                      "gid1")
        timer2       (timers.db/create-if-authorized! (get gen-projects "goo")
                                                      "gid2")]
    (testing "Owned timer"
      (testing "Started"
        (let [timer-id           (:id timer1)
              current-time       (util/current-epoch-seconds)
              _                  (timers.db/start-if-authorized!
                                  timer-id
                                  current-time
                                  "gid1")
              ;; Stop the timer 10 seconds later.
              stop-time          (+ current-time 10.0)
              {:keys [duration]} (timers.db/stop-if-authorized!
                                  timer-id
                                  stop-time
                                  "gid1")]
          (is (= (TimePeriod. 0 0 10.0)
                 duration))))
      (testing "Not started"
        (let [timer-id    (:id timer1)
              stop-result (timers.db/stop-if-authorized!
                           timer-id
                           (util/current-epoch-seconds)
                           "gid1")]
          (is (nil? stop-result)))))

    (testing "Unowned timer"
      (let [timer-id    (:id timer2)
            stop-result (timers.db/stop-if-authorized!
                         timer-id
                         (util/current-epoch-seconds)
                         "gid1")]
        (is (nil? stop-result))))))
