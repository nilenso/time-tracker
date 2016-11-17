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

(use-fixtures :once fixtures/init! fixtures/migrate-test-db)
(use-fixtures :each fixtures/isolate-db)

(defn- contains-map?
  "True if all key-value pairs in m2 are in m1."
  [m1 m2]
  (= m2 (select-keys m1 (keys m2))))


(deftest create
  (let [gen-projects  (projects.helpers/populate-data! {"gid1" ["foo"]
                                                        "gid2" ["goo"]})
        project-id    (get gen-projects "foo")
        created-timer (timers.db/create! (db/connection)
                                         project-id
                                         "gid1")
        actual-timer  (first (jdbc/find-by-keys (db/connection) "timer"
                                                {"project_id" project-id}))]
    (is (some? created-timer))
    (is (some? actual-timer))
    (is (contains-map? actual-timer created-timer))))


(deftest has-timing-access?
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})]
    (testing "You can time a project if you have admin access"
      (is (timers.db/has-timing-access? (db/connection)
                                        "gid1"
                                        (get gen-projects "foo")))
      (is (timers.db/has-timing-access? (db/connection)
                                        "gid2"
                                        (get gen-projects "goo"))))

    (testing "Cannot time a project without admin access"
      (is (not (timers.db/has-timing-access? (db/connection)
                                             "gid2"
                                             (get gen-projects "foo"))))
      (is (not (timers.db/has-timing-access? (db/connection)
                                             "gid1"
                                             (get gen-projects "goo")))))))


(deftest owns?
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})
        timer1 (timers.db/create! (db/connection)
                                  (get gen-projects "foo")
                                  "gid1")
        timer2 (timers.db/create! (db/connection)
                                  (get gen-projects "goo")
                                  "gid2")]
    (testing "If you create a timer you own it"
      (is (timers.db/owns? (db/connection)
                           "gid1"
                           (:id timer1)))
      (is (timers.db/owns? (db/connection)
                           "gid2"
                           (:id timer2))))

    (testing "You don't own timers someone else created"
      (is (not (timers.db/owns? (db/connection)
                                "gid1"
                                (:id timer2))))
      (is (not (timers.db/owns? (db/connection)
                                "gid2"
                                (:id timer1)))))))

(deftest update-duration-if-authorized
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})
        timer1       (timers.db/create! (db/connection)
                                        (get gen-projects "foo")
                                        "gid1")
        timer3       (timers.db/create! (db/connection)
                                        (get gen-projects "foo")
                                        "gid1")]
    (testing "Not started"
      (let [timer-id        (:id timer1)
            current-time    (util/current-epoch-seconds)
            updated-project (timers.db/update-duration!
                             (db/connection)
                             timer-id
                             (TimePeriod. 0 7 0.0)
                             current-time)]
        (is (= (TimePeriod. 0 7 0.0)
               (:duration updated-project)))
        (is (nil? (:started_time updated-project)))))

    (testing "Started"
      (let [timer-id        (:id timer3)
            start-result    (timers.db/start!
                             (db/connection)
                             timer-id
                             (util/current-epoch-seconds))
            current-time    (util/current-epoch-seconds)
            updated-project (timers.db/update-duration!
                             (db/connection)
                             timer-id
                             (TimePeriod. 0 9 0.0)
                             current-time)]
        (is (some? start-result))
        (is (= (TimePeriod. 0 9 0.0)
               (:duration updated-project)))
        (is (=  current-time
                (-> (:started_time updated-project)
                    (util/to-epoch-seconds))))))))


(defn- create-timers!
  "Creates n timers for each project and returns a set of their ids."
  [connection google-id gen-projects project-names n]
  (set (for [project-id (map gen-projects project-names)
             _          (range n)]
         (:id (timers.db/create!
               connection
               project-id
               google-id)))))


(deftest retrieve-authorized-timers
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo" "goo"]
                                                       "gid2" ["bar" "baz"]})
        expected1    (create-timers! (db/connection) "gid1" gen-projects ["foo" "goo"] 2)
        expected2    (create-timers! (db/connection) "gid2" gen-projects ["bar" "baz"] 2)
        actual1      (->> (timers.db/retrieve-authorized-timers (db/connection) "gid1")
                          (map :id)
                          (set))
        actual2      (->> (timers.db/retrieve-authorized-timers (db/connection) "gid2")
                          (map :id)
                          (set))]
    (is (= expected1 actual1))
    (is (= expected2 actual2))))


(deftest delete
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})
        timer1       (timers.db/create! (db/connection)
                                        (get gen-projects "foo")
                                        "gid1")
        timer2       (timers.db/create! (db/connection)
                                        (get gen-projects "goo")
                                        "gid2")]

    (testing "Owned timer"
      (let [timer-id     (:id timer1)
            deleted-bool (timers.db/delete! (db/connection) timer-id)
            actual-timer (jdbc/get-by-id (db/connection) "timer" timer-id)]
        (is deleted-bool)
        (is (nil? actual-timer))))))


(deftest start
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})
        timer1       (timers.db/create! (db/connection)
                                        (get gen-projects "foo")
                                        "gid1")
        timer2       (timers.db/create! (db/connection)
                                        (get gen-projects "goo")
                                        "gid2")
        timer3       (timers.db/create! (db/connection)
                                        (get gen-projects "foo")
                                        "gid1")]
    (testing "Not started"
      (let [timer-id     (:id timer1)
            current-time (util/current-epoch-seconds)
            start-result (timers.db/start!
                          (db/connection)
                          timer-id
                          current-time)]
        (is (= current-time
               (util/to-epoch-seconds (:started_time start-result))))))
    (testing "Started"
      (let [timer-id      (:id timer2)
            first-result  (timers.db/start!
                           (db/connection)
                           timer-id
                           (util/current-epoch-seconds))
            second-result (timers.db/start!
                           (db/connection)
                           timer-id
                           (util/current-epoch-seconds))]
        (is (some? first-result))
        (is (nil? second-result))))))


(deftest stop
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})
        timer1       (timers.db/create! (db/connection)
                                        (get gen-projects "foo")
                                        "gid1")
        timer2       (timers.db/create! (db/connection)
                                        (get gen-projects "goo")
                                        "gid2")]
    (testing "Started"
      (let [timer-id           (:id timer1)
            current-time       (util/current-epoch-seconds)
            _                  (timers.db/start!
                                (db/connection)
                                timer-id
                                current-time)
            ;; Stop the timer 10 seconds later.
            stop-time          (+ current-time 10.0)
            {:keys [duration]} (timers.db/stop!
                                (db/connection)
                                timer-id
                                stop-time)]
        (is (= (TimePeriod. 0 0 10.0)
               duration))))
    
    (testing "Not started"
      (let [timer-id    (:id timer1)
            stop-result (timers.db/stop!
                         (db/connection)
                         timer-id
                         (util/current-epoch-seconds))]
        (is (nil? stop-result))))))
