(ns time-tracker.projects.db-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.projects.db :as projects.db]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.projects.test-helpers :as projects.helpers]
            [time-tracker.users.test-helpers :as users.helpers]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db)
(use-fixtures :each fixtures/isolate-db)


(deftest retrieve-if-authorized
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})]

    (testing "Authorized project"
      (let [project          (projects.db/retrieve-if-authorized
                              (get gen-projects "foo")
                              "gid1")
            expected-project {:id (get gen-projects "foo") :name "foo"}]
        (is (= expected-project project))))

    (testing "Unauthorized project"
      (let [project (projects.db/retrieve-if-authorized
                     (get gen-projects "foo")
                     "gid2")]
        (is (nil? project))))))


(deftest update-if-authorized
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})]

    (testing "Authorized project"
      (let [project-id       (get gen-projects "foo")
            updated-project  (projects.db/update-if-authorized!
                              project-id
                              {:name "Dondochakka"}
                              "gid1")
            expected-project {:id project-id :name "Dondochakka"}
            actual-project   (jdbc/get-by-id (db/connection) "project" project-id)]
        (is (= expected-project updated-project))
        (is (= actual-project updated-project))))

    (testing "Unauthorized project"
      (let [project-id        (get gen-projects "goo")
            updated-project   (projects.db/update-if-authorized!
                               project-id
                               {:name "Chappy the rabbit"}
                               "gid1")
            unchanged-project {:id project-id :name "goo"}
            actual-project    (jdbc/get-by-id (db/connection) "project" project-id)]
        (is (nil? updated-project))
        (is (= unchanged-project actual-project))))))


(deftest delete-if-authorized
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]
                                                       "gid2" ["goo"]})]

    (testing "Authorized project"
      (let [project-id     (get gen-projects "foo")
            deleted-bool   (projects.db/delete-if-authorized! project-id "gid1")
            actual-project (jdbc/get-by-id (db/connection) "project" project-id)]
        (is deleted-bool)
        (is (nil? actual-project))))

    (testing "Unauthorized project"
      (let [project-id       (get gen-projects "goo")
            deleted-bool     (projects.db/delete-if-authorized! project-id "gid1")
            actual-project   (jdbc/get-by-id (db/connection) "project" project-id)
            expected-project {:id project-id :name "goo"}]
        (is (not deleted-bool))
        (is (= expected-project actual-project))))))


(deftest retrieve-authorized-projects
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo" "goo"]
                                                       "gid2" ["bar" "baz"]})]

    (testing "The first user"
      (let [projects      (projects.db/retrieve-authorized-projects "gid1")
            project-names (map :name projects)]
        (is (= (sort ["foo" "goo"])
               (sort project-names)))))

    (testing "The second user"
      (let [projects      (projects.db/retrieve-authorized-projects "gid2")
            project-names (map :name projects)]
        (is (= (sort ["bar" "baz"])
               (sort project-names)))))))


(deftest create-if-authorized
  (users.helpers/create-users! ["Sai Abdul" "gid1" "admin"]
                               ["Paul Graham" "gid2" "user"])
  
  (testing "Authorized user"
    (let [created-project (projects.db/create-if-authorized!
                           {:name "foo"}
                           "gid1")
          actual-project  (first (jdbc/find-by-keys (db/connection) "project"
                                                    {"name" "foo"}))]
      (is (some? created-project))
      (is (some? actual-project))
      (is (= actual-project created-project))
      (is (= "foo" (:name created-project)))))

  (testing "Unauthorized user"
    (let [created-project (projects.db/create-if-authorized!
                           {:name "goo"}
                           "gid2")
          actual-project  (first (jdbc/find-by-keys (db/connection) "project"
                                                    {"name" "goo"}))]
      (is (nil? created-project))
      (is (nil? actual-project)))))
