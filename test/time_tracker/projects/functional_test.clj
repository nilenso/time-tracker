(ns time-tracker.projects.functional-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.db :as db]
            [time-tracker.projects.test-helpers :as projects.helpers]
            [time-tracker.users.test-helpers :as users.helpers]
            [time-tracker.auth.core :as auth]
            [time-tracker.test-helpers :as helpers]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)

(def project-api-format (str/join [helpers/test-api "projects/%s/"]))
(def projects-api (str/join [helpers/test-api "projects/"]))

(deftest retrieve-single-project-test
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]})
        project-id   (get gen-projects "foo")
        url          (format project-api-format project-id)]

    (testing "Authorized"
      (let [{:keys [status body]} (helpers/http-request :get url "gid1")
            expected-body         {"id" project-id "name" "foo"}]
        (is (= status 200))
        (is (= expected-body body))))

    (testing "Unauthorized"
      (let [{:keys [status]} (helpers/http-request :get url "gid2")]
        (is (= status 403))))))


(deftest update-single-project-test
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]})
        project-id   (get gen-projects "foo")
        url          (format project-api-format project-id)] 

    (testing "Authorized"
      (let [{:keys [status body]} (helpers/http-request :put url "gid1" {"name" "goo"})
            expected-body         {"id" project-id "name" "goo"}]
        (is (= status 200))
        (is (= expected-body body))))

    (testing "Unauthorized"
      (let [{:keys [status]} (helpers/http-request :put url "gid2" {"name" "bar"})]
        (is (= status 403))))))


(deftest delete-single-project-test
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo" "goo"]})
        format-url   project-api-format]

    (testing "Authorized"
      (let [project-id       (get gen-projects "foo")
            url              (format format-url project-id)
            {:keys [status]} (helpers/http-request :delete url "gid1")]
        (is (= 204 status))))

    (testing "Unauthorized"
      (let [project-id       (get gen-projects "goo")
            url              (format format-url project-id)
            {:keys [status]} (helpers/http-request :delete url "gid2")]
        (is (= 403 status))))))


(deftest retrieve-all-authorized-projects-test
  (println "retrieve-all-authorized-projects-test is currently disabled!")
  #_(let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo" "goo"]
                                              "gid2" ["bar" "baz"]})
        url          "http://localhost:8000/api/projects/"]

    (testing "User 1"
      (let [{:keys [status body]} (helpers/http-request :get url "gid1")
            project-names         (map #(get % "name") body)]
        (is (= 200 status))
        (is (= (sort ["foo" "goo"])
               (sort project-names)))))

    (testing "User 2"
      (let [{:keys [status body]} (helpers/http-request :get url "gid2")
            project-names         (map #(get % "name") body)]
        (is (= 200 status))
        (is (= (sort ["bar" "baz"])
               (sort project-names)))))))


(deftest create-project-test
  (users.helpers/create-users! ["Sai Abdul" "gid1" "admin"]
                               ["Paul Graham" "gid2" "user"])
  (let [url projects-api]

    (testing "Admin user"
      (let [{:keys [status body]} (helpers/http-request :post url "gid1"
                                                        {"name" "foo"})]
        (is (= 201 status))
        (is (= "foo"
               (get body "name")))))

    (testing "Pleb user"
      (let [{:keys [status body]} (helpers/http-request :post url "gid2"
                                                        {"name" "javascript is the best"})]
        (is (= 403 status))))))
