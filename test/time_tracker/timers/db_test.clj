(ns time-tracker.timers.db-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.projects.test-helpers :as projects.helpers]
            [time-tracker.clients.test-helpers :as clients.helpers]
            [time-tracker.tasks.test-helpers :as tasks.helpers]
            [time-tracker.users.test-helpers :as users.helpers]
            [time-tracker.util :as util]
            [clj-time.jdbc]
            [clj-time.coerce :as time.coerce]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db)
(use-fixtures :each fixtures/isolate-db)

(defn- contains-map?
  "True if all key-value pairs in m2 are in m1."
  [m1 m2]
  (= m2 (select-keys m1 (keys m2))))

(defn populate-db
  [google-id]
  (let [client-name "FooClient"
        client-id (:id (clients.helpers/create-client! (db/connection) {:name client-name}))
        project-name->project-ids (projects.helpers/populate-data! {google-id ["pr1" "pr2"]}
                                                                   client-id)
        task-ids (map (fn [task-name project-id]
                        (tasks.helpers/create-task! (db/connection)
                                                    task-name
                                                    project-id))
                      ["task1" "task2"]
                      (vals project-name->project-ids))]
    {:task-ids task-ids
     :project-name->project-ids project-name->project-ids}))


(deftest create-test
  (let [google-id "gid1"
        task-ids (:task-ids (populate-db google-id))
        task-id (first task-ids)
        current-time  (util/current-epoch-seconds)
        created-timer (timers-db/create! (db/connection)
                                         task-id
                                         google-id
                                         current-time
                                         "thunne")
        actual-timer  (timers-db/transform-timer-map
                       (first (jdbc/find-by-keys (db/connection) "timer"
                                                 {"task_id" task-id})))]
    (is (some? created-timer))
    (is (some? actual-timer))
    (is (contains-map? actual-timer created-timer))))


(deftest has-timing-access?-test
  (let [task-ids1 (:task-ids (populate-db "gid1"))
        task-ids2 (:task-ids (populate-db "gid2"))]
    (testing "You can time a project if you have admin access"
      (is (timers-db/has-timing-access? (db/connection)
                                        "gid1"
                                        (first task-ids1)))
      (is (timers-db/has-timing-access? (db/connection)
                                        "gid2"
                                        (first task-ids2))))

    (testing "Cannot time a project without admin access"
      (is (not (timers-db/has-timing-access? (db/connection)
                                             "gid2"
                                             (first task-ids1))))
      (is (not (timers-db/has-timing-access? (db/connection)
                                             "gid1"
                                             (first task-ids2)))))))


(deftest owns?-test
  (let [task-id1 (first (:task-ids (populate-db "gid1")))
        task-id2 (first (:task-ids (populate-db "gid2")))
        timer1 (timers-db/create! (db/connection)
                                  task-id1
                                  "gid1"
                                  (util/current-epoch-seconds)
                                  "")
        timer2 (timers-db/create! (db/connection)
                                  task-id2
                                  "gid2"
                                  (util/current-epoch-seconds)
                                  "")]
    (testing "If you create a timer you own it"
      (is (timers-db/owns? (db/connection)
                           "gid1"
                           (:id timer1)))
      (is (timers-db/owns? (db/connection)
                           "gid2"
                           (:id timer2))))

    (testing "You don't own timers someone else created"
      (is (not (timers-db/owns? (db/connection)
                                "gid1"
                                (:id timer2))))
      (is (not (timers-db/owns? (db/connection)
                                "gid2"
                                (:id timer1)))))))

(deftest update-test
  (let [google-id "gid1"
        task-id (first (:task-ids (populate-db google-id)))
        timer1       (timers-db/create! (db/connection)
                                        task-id
                                        google-id
                                        (util/current-epoch-seconds)
                                        "")
        timer3       (timers-db/create! (db/connection)
                                        task-id
                                        google-id
                                        (util/current-epoch-seconds)
                                        "baz")]
    (testing "Not started"
      (let [timer-id        (:id timer1)
            current-time    (util/current-epoch-seconds)
            updated-project (timers-db/update!
                             (db/connection)
                             timer-id
                             (* 7 60)
                             current-time
                             "astro zombies")]
        (is (= (* 7 60)
               (:duration updated-project)))
        (is (nil? (:started-time updated-project)))
        (is (= "astro zombies"
               (:notes updated-project)))))

    (testing "Started"
      (let [timer-id        (:id timer3)
            start-result    (timers-db/start!
                             (db/connection)
                             timer-id
                             (util/current-epoch-seconds))
            current-time    (util/current-epoch-seconds)
            updated-project (timers-db/update!
                             (db/connection)
                             timer-id
                             (* 9 60)
                             current-time
                             "hermaeus mora")]
        (is (some? start-result))
        (is (= (* 9 60)
               (:duration updated-project)))
        (is (=  current-time
                (:started-time updated-project)))
        (is (= "hermaeus mora"
               (:notes updated-project)))))))


(defn- create-timers!
  "Creates n timers for each task id and returns a set of their ids."
  [connection google-id task-ids n]
  (let [current-time (util/current-epoch-seconds)]
    (set (for [task-id task-ids
               _          (range n)]
           (:id (timers-db/create!
                 connection
                 task-id
                 google-id
                 current-time
                 ""))))))


(deftest retrieve-all-test
  (let [task-ids1 (:task-ids (populate-db "gid1"))
        task-ids2 (:task-ids (populate-db "gid2"))
        expected1    (create-timers! (db/connection) "gid1" task-ids1 2)
        expected2    (create-timers! (db/connection) "gid2" task-ids2 2)
        expected-ids (into expected1 expected2)
        actual (->> (timers-db/retrieve-all (db/connection))
                    (map :id)
                    (set))]
    (is (= expected-ids actual))))


(deftest retrieve-between-test
  (let [task-ids (:task-ids (populate-db "gid1"))
        current-time (util/current-epoch-seconds)
        recent-time  (- current-time 10)
        older-time   (- current-time 2400)
        recent-timer (timers-db/create! (db/connection)
                                        (first task-ids)
                                        "gid1"
                                        recent-time
                                        "")
        older-timer  (timers-db/create! (db/connection)
                                        (second task-ids)
                                        "gid1"
                                        older-time
                                        "")]
    (testing "start-epoch is inclusive and end-epoch is exclusive"
      (let [result-timers (timers-db/retrieve-between (db/connection) older-time recent-time)]
        (is (= #{(:id older-timer)}
               (->> result-timers
                    (map :id)
                    (set))))))

    (testing "timers should be between the epochs"
      (let [result-timers (timers-db/retrieve-between (db/connection)
                                                      older-time
                                                      (+ recent-time 1))]
        (is (= #{(:id older-timer)
                 (:id recent-timer)}
               (->> result-timers
                    (map :id)
                    (set)))))
      (let [result-timers (timers-db/retrieve-between (db/connection)
                                                      recent-time
                                                      (+ recent-time 1))]
        (is (= #{(:id recent-timer)}
               (->> result-timers
                    (map :id)
                    (set)))))
      (let [result-timers (timers-db/retrieve-between (db/connection)
                                                      recent-time
                                                      recent-time)]
        (is (empty? result-timers))))))


(deftest retrieve-between-authorized-test
  (let [task-ids1 (:task-ids (populate-db "gid1"))
        task-ids2 (:task-ids (populate-db "gid2"))

        current-time        (util/current-epoch-seconds)
        recent-time          (- current-time 10)
        older-time           (- current-time 2400)
        recent-timer         (timers-db/create! (db/connection)
                                                (first task-ids1)
                                                "gid1"
                                                recent-time
                                                "")
        older-timer          (timers-db/create! (db/connection)
                                                (second task-ids1)
                                                "gid1"
                                                older-time
                                                "")
        somebody-elses-timer (timers-db/create! (db/connection)
                                                (first task-ids2)
                                                "gid2"
                                                older-time
                                                "")]
    (testing "start-epoch is inclusive and end-epoch is exclusive"
      (let [result-timers (timers-db/retrieve-between-authorized (db/connection)
                                                                 "gid1"
                                                                 older-time
                                                                 recent-time)]
        (is (= #{(:id older-timer)}
               (->> result-timers
                    (map :id)
                    (set))))))

    (testing "timers should be between the epochs"
      (let [result-timers (timers-db/retrieve-between-authorized (db/connection)
                                                                 "gid1"
                                                                 older-time
                                                                 (+ recent-time 1))]
        (is (= #{(:id older-timer)
                 (:id recent-timer)}
               (->> result-timers
                    (map :id)
                    (set)))))
      (let [result-timers (timers-db/retrieve-between-authorized (db/connection)
                                                                 "gid1"
                                                                 recent-time
                                                                 (+ recent-time 1))]
        (is (= #{(:id recent-timer)}
               (->> result-timers
                    (map :id)
                    (set)))))
      (let [result-timers (timers-db/retrieve-between-authorized (db/connection)
                                                                 "gid1"
                                                                 recent-time
                                                                 recent-time)]
        (is (empty? result-timers))))

    (testing "retrieving the other person's timers"
      (let [result-timers (timers-db/retrieve-between-authorized (db/connection)
                                                                 "gid2"
                                                                 older-time
                                                                 recent-time)]
        (is (= #{(:id somebody-elses-timer)}
               (->> result-timers
                    (map :id)
                    (set))))))))


(deftest retrieve-authorized-timers-test
  (let [task-ids1 (:task-ids (populate-db "gid1"))
        task-ids2 (:task-ids (populate-db "gid2"))
        expected1    (create-timers! (db/connection) "gid1" task-ids1 2)
        expected2    (create-timers! (db/connection) "gid2" task-ids2 2)
        actual1      (->> (timers-db/retrieve-authorized-timers (db/connection) "gid1")
                          (map :id)
                          (set))
        actual2      (->> (timers-db/retrieve-authorized-timers (db/connection) "gid2")
                          (map :id)
                          (set))]
    (is (= expected1 actual1))
    (is (= expected2 actual2))))


(deftest retrieve-started-timers-test
  (let [task-ids (:task-ids (populate-db "gid1"))
        created-timers (create-timers! (db/connection) "gid1" task-ids 6)
        current-time   (util/current-epoch-seconds)]
    ;; Start the first five timers
    (doall (map #(timers-db/start! (db/connection) % current-time)
                (take 5 created-timers)))
    (let [started-timers (timers-db/retrieve-started-timers (db/connection) "gid1")]
      (is (= 5 (count started-timers)))
      (doseq [timer started-timers]
        (is (not (nil? (:started-time timer))))))))


(deftest delete-test
  (let [task-id (first (:task-ids (populate-db "gid1")))
        current-time (util/current-epoch-seconds)
        timer       (timers-db/create! (db/connection)
                                        task-id
                                        "gid1"
                                        current-time
                                        "")]

    (testing "Owned timer"
      (let [timer-id     (:id timer)
            deleted-bool (timers-db/delete! (db/connection) timer-id)
            actual-timer (jdbc/get-by-id (db/connection) "timer" timer-id)]
        (is deleted-bool)
        (is (nil? actual-timer))))))


(deftest start-test
  (let [task-ids (:task-ids (populate-db "gid1"))
        _ (populate-db "gid2") ;; populate-db also creates a user with given gid
        timer1       (timers-db/create! (db/connection)
                                        (first task-ids)
                                        "gid1"
                                        (util/current-epoch-seconds)
                                        "")
        timer2       (timers-db/create! (db/connection)
                                        (second task-ids)
                                        "gid2"
                                        (util/current-epoch-seconds)
                                        "")
        timer3       (timers-db/create! (db/connection)
                                        (second task-ids)
                                        "gid1"
                                        (util/current-epoch-seconds)
                                        "")]
    (testing "Not started"
      (let [timer-id     (:id timer1)
            current-time (util/current-epoch-seconds)
            start-result (timers-db/start!
                          (db/connection)
                          timer-id
                          current-time)]
        (is (= current-time
               (:started-time start-result)))))
    (testing "Started"
      (let [timer-id      (:id timer2)
            first-result  (timers-db/start!
                           (db/connection)
                           timer-id
                           (util/current-epoch-seconds))
            second-result (timers-db/start!
                           (db/connection)
                           timer-id
                           (util/current-epoch-seconds))]
        (is (some? first-result))
        (is (nil? second-result))))))


(deftest stop-test
  (let [task-ids (:task-ids (populate-db "gid1"))
        _ (populate-db "gid2") ;; populate-db also creates a user with given gid
        current-time (util/current-epoch-seconds)
        timer1       (timers-db/create! (db/connection)
                                        (first task-ids)
                                        "gid1"
                                        current-time
                                        "")
        timer2       (timers-db/create! (db/connection)
                                        (second task-ids)
                                        "gid2"
                                        current-time
                                        "")]
    (testing "Started"
      (let [timer-id           (:id timer1)
            current-time       (util/current-epoch-seconds)
            _                  (timers-db/start!
                                (db/connection)
                                timer-id
                                current-time)
            ;; Stop the timer 10 seconds later.
            stop-time          (+ current-time 10.0)
            {:keys [duration]} (timers-db/stop!
                                (db/connection)
                                timer-id
                                stop-time)]
        (is (= 10
               duration))))

    (testing "Not started"
      (let [timer-id    (:id timer1)
            stop-result (timers-db/stop!
                         (db/connection)
                         timer-id
                         (util/current-epoch-seconds))]
        (is (nil? stop-result))))))
