(ns time-tracker.projects.db-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            
            [time-tracker.db :as db]
            [time-tracker.projects.db :as projects.db]
            [time-tracker.fixtures :as fixtures]))

(use-fixtures :once fixtures/migrate-test-db)
(use-fixtures :each fixtures/isolate-db)


(defn populate-data!
  "In: {google-id [list of owned projects]}
  Out: {project-name project-id} 
  Fills out the database with the given test data."
  [test-data]
  (jdbc/with-db-transaction [conn (db/connection)]
    (reduce (fn [project-name-ids [google-id project-names]]
              (let [{user-id :id} (first (jdbc/insert! conn "app_user"
                                                       {"google_id" google-id
                                                        "name" "Agent Smith"}))]
                (merge project-name-ids
                       (->> (for [project-name project-names]
                              (let [{project-id :id} (first (jdbc/insert! conn "project"
                                                                          {"name" project-name}))]
                                (jdbc/execute! conn
                                               [(str "INSERT INTO project_permission "
                                                     "(project_id, app_user_id, permissions) "
                                                     "VALUES (?, ?, ARRAY['admin']::permission[]);")
                                                project-id user-id]
                                               {:multi? false})
                                [project-name project-id]))
                            (into {})))))
            {}
            test-data)))


(deftest retrieve-project-if-authorized
  (let [gen-projects (populate-data! {"gid1" ["foo"]
                                      "gid2" ["goo"]})]
    (testing "Authorized project"
      (let [project (projects.db/retrieve-project-if-authorized
                     (get gen-projects "foo")
                     "gid1")
            expected-project {:id (get gen-projects "foo") :name "foo"}]
        (is (= expected-project project))))

    (testing "Unauthorized project"
      (let [project (projects.db/retrieve-project-if-authorized
                     (get gen-projects "foo")
                     "gid2")]
        (is (nil? project))))))


(deftest update-project-if-authorized
  (let [gen-projects (populate-data! {"gid1" ["foo"]
                                      "gid2" ["goo"]})]
    (testing "Authorized project"
      (let [project-id (get gen-projects "foo")
            updated-project (projects.db/update-project-if-authorized!
                             project-id
                             {:name "Dondochakka"}
                             "gid1")
            expected-project {:id project-id :name "Dondochakka"}
            actual-project (jdbc/get-by-id (db/connection) "project" project-id)]
        (is (= expected-project updated-project))
        (is (= actual-project updated-project))))

    (testing "Unauthorized project"
      (let [project-id (get gen-projects "goo")
            updated-project (projects.db/update-project-if-authorized!
                             project-id
                             {:name "Chappy the rabbit"}
                             "gid1")
            unchanged-project {:id project-id :name "goo"}
            actual-project (jdbc/get-by-id (db/connection) "project" project-id)]
        (is (nil? updated-project))
        (is (= unchanged-project actual-project))))))


(deftest delete-project-if-authorized
  (let [gen-projects (populate-data! {"gid1" ["foo"]
                                      "gid2" ["goo"]})]
    (testing "Authorized project"
      (let [project-id (get gen-projects "foo")
            deleted-bool (projects.db/delete-project-if-authorized! project-id "gid1")
            actual-project (jdbc/get-by-id (db/connection) "project" project-id)]
        (is deleted-bool)
        (is (nil? actual-project))))

    (testing "Unauthorized project"
      (let [project-id (get gen-projects "goo")
            deleted-bool (projects.db/delete-project-if-authorized! project-id "gid1")
            actual-project (jdbc/get-by-id (db/connection) "project" project-id)
            expected-project {:id project-id :name "goo"}]
        (is (not deleted-bool))
        (is (= expected-project actual-project))))))


(deftest retrieve-authorized-projects
  (let [gen-projects (populate-data! {"gid1" ["foo" "goo"]
                                      "gid2" ["bar" "baz"]})]
    (testing "The first user"
      (let [projects (projects.db/retrieve-authorized-projects "gid1")
            project-names (map :name projects)]
        (is (= (sort ["foo" "goo"])
               (sort project-names)))))

    (testing "The second user"
      (let [projects (projects.db/retrieve-authorized-projects "gid2")
            project-names (map :name projects)]
        (is (= (sort ["bar" "baz"])
               (sort project-names)))))))


(deftest create-project-if-authorized
  (jdbc/execute! (db/connection)
                 [(str "INSERT INTO app_user "
                       "(name, google_id, role) "
                       "VALUES (?, ?, ?::user_role);")
                  ["Sai Abdul" "gid1" "admin"]
                  ["Paul Graham" "gid2" "user"]]
                 {:multi? true})
    
  (testing "Authorized user"
    (let [created-project (projects.db/create-project-if-authorized!
                           {:name "foo"}
                           "gid1")
          actual-project (first (jdbc/find-by-keys (db/connection) "project"
                                                   {"name" "foo"}))]
      (is (some? created-project))
      (is (some? actual-project))
      (is (= actual-project created-project))
      (is (= "foo" (:name created-project)))))

  (testing "Unauthorized user"
    (let [created-project (projects.db/create-project-if-authorized!
                           {:name "goo"}
                           "gid2")
          actual-project (first (jdbc/find-by-keys (db/connection) "project"
                                                   {"name" "goo"}))]
      (is (nil? created-project))
      (is (nil? actual-project)))))
