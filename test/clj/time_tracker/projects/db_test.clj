(ns time-tracker.projects.db-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.projects.db :as projects.db]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.clients.test-helpers :as clients.helpers]
            [time-tracker.projects.test-helpers :as projects.helpers]
            [time-tracker.users.test-helpers :as users.helpers]))

(use-fixtures :once fixtures/init!)
(use-fixtures :each fixtures/isolate-db)

(deftest retrieve-test
  (let [client-id (:id (clients.helpers/create-client! (db/connection) {:name "FooClient"}))
        gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]}
                                                      client-id)
        project          (projects.db/retrieve
                          (db/connection)
                          (get gen-projects "foo"))
        expected-project {:id (get gen-projects "foo") :name "foo" :client_id client-id}]
    (is (= expected-project project))))


(deftest update-test
  (let [client-id (:id (clients.helpers/create-client! (db/connection) {:name "FooClient"}))
        gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]}
                                                      client-id)]
    (let [project-id       (get gen-projects "foo")
          updated-project  (projects.db/update!
                            (db/connection)
                            project-id
                            {:name "Dondochakka"})
          expected-project {:id project-id :name "Dondochakka" :client_id client-id}
          actual-project   (jdbc/get-by-id (db/connection) "project" project-id)]
      (is (= expected-project updated-project))
      (is (= actual-project updated-project)))))


(deftest delete-test
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})]
    (let [project-id     (get gen-projects "foo")
          deleted-bool   (projects.db/delete!
                          (db/connection) project-id)
          actual-project (jdbc/get-by-id (db/connection) "project" project-id)]
      (is deleted-bool)
      (is (nil? actual-project)))))


(deftest retrieve-authorized-projects-test
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo" "goo"]
                                                       "gid2" ["bar" "baz"]})]

    (testing "The first user"
      (let [projects      (projects.db/retrieve-authorized-projects
                           (db/connection) "gid1")
            project-names (map :name projects)]
        (is (= (sort ["foo" "goo"])
               (sort project-names)))))

    (testing "The second user"
      (let [projects      (projects.db/retrieve-authorized-projects
                           (db/connection) "gid2")
            project-names (map :name projects)]
        (is (= (sort ["bar" "baz"])
               (sort project-names)))))))

(deftest retrieve-all-test
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo" "goo"]
                                                       "gid2" ["bar" "baz"]})]

    (testing "All projects should be retrieved"
      (let [projects      (projects.db/retrieve-all (db/connection))
            project-names (map :name projects)]
        (is (= (sort ["foo" "goo" "bar" "baz"])
               (sort project-names)))))))

(deftest create-test
  (users.helpers/create-users! ["Sai Abdul" "gid1" "admin" "sai@sai.com"]
                               ["Paul Graham" "gid2" "user"])
  (let [client-id (:id (clients.helpers/create-client! (db/connection) {:name "FooClient"}))
        created-project (projects.db/create!
                         (db/connection)
                         {:name "foo"
                          :client-id client-id})
        actual-project  (first (jdbc/find-by-keys (db/connection) "project"
                                                  {"name" "foo"}))]
    (is (some? created-project))
    (is (some? actual-project))
    (is (= actual-project created-project))
    (is (= "foo" (:name created-project)))))
