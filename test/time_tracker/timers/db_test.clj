(ns time-tracker.timers.db-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers.db]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.projects.test-helpers :as projects.helpers]
            [time-tracker.users.test-helpers :as users.helpers]
            [clj-time.jdbc]))

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
