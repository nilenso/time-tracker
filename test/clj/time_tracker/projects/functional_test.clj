(ns time-tracker.projects.functional-test
  (:require [clojure.test :refer :all]
            [clojure.string :as s]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.db :as db]
            [time-tracker.clients.test-helpers :as clients.helpers]
            [time-tracker.projects.test-helpers :as projects.helpers]
            [time-tracker.users.test-helpers :as users.helpers]
            [time-tracker.auth.core :as auth]
            [time-tracker.test-helpers :as helpers]))

(use-fixtures :once fixtures/init! fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)

(defn- project-api-format
  []
  (s/join [(helpers/settings :api-root) "projects/%s/"]))

(defn- projects-api
  []
  (s/join [(helpers/settings :api-root) "projects/"]))

(deftest retrieve-single-project-test
  (let [client-id (:id (clients.helpers/create-client! (db/connection) {:name "FooClient"}))
        gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]} client-id)
        project-id   (get gen-projects "foo")
        url          (format (project-api-format) project-id)]

    (testing "Authorized"
      (let [{:keys [status body]} (helpers/http-request :get url "gid1")
            expected-body         {"id" project-id
                                   "name" "foo"
                                   "client_id" client-id}]
        (is (= status 200))
        (is (= expected-body body))))

    (testing "Unauthorized"
      (let [{:keys [status]} (helpers/http-request :get url "gid2")]
        (is (= status 403))))))


(deftest update-single-project-test
  (let [client-id (:id (clients.helpers/create-client! (db/connection) {:name "FooClient"}))
        gen-projects (projects.helpers/populate-data! {"gid1" ["foo"]} client-id)
        project-id   (get gen-projects "foo")
        url          (format (project-api-format) project-id)]

    (testing "Authorized"
      (let [{:keys [status body]} (helpers/http-request :put url "gid1" {"name" "goo"})
            expected-body         {"id" project-id
                                   "name" "goo"
                                   "client_id" client-id}]
        (is (= status 200))
        (is (= expected-body body))))

    (testing "Unauthorized"
      (let [{:keys [status]} (helpers/http-request :put url "gid2" {"name" "bar"})]
        (is (= status 403))))

    (testing "Can not set project name as empty"
      (let [{:keys [status]} (helpers/http-request :put url "gid1" {"name" ""})]
        (is (= status 400))))))


(deftest delete-single-project-test
  (let [gen-projects (projects.helpers/populate-data! {"gid1" ["foo" "goo"]})
        format-url   (project-api-format)]

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
  (users.helpers/create-users! ["Sai Abdul" "gid1" "admin" "sai@abdul.com"]
                               ["Paul Graham" "gid2" "user" "pg@pg.com"])
  (let [url (projects-api)
        client-id (:id (clients.helpers/create-client! (db/connection) {:name "FooClient"}))]

    (testing "Admin user"
      (let [{:keys [status body]} (helpers/http-request :post url "gid1"
                                                        {"name" "foo"
                                                         "client-id" client-id})]
        (is (= 201 status))
        (is (= "foo"
               (get body "name")))))

    (testing "Pleb user"
      (let [{:keys [status body]} (helpers/http-request :post url "gid2"
                                                        {"name" "javascript is the best"
                                                         "client-id" client-id})]
        (is (= 403 status))))

    (testing "Can not create a project with empty name"
      (let [{:keys [status body]} (helpers/http-request :post url "gid1"
                                                        {"name" ""
                                                         "client-id" client-id})]
        (is (= 400 status))))))
