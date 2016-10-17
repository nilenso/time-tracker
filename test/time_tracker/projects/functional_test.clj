(ns time-tracker.projects.functional-test
  (:require [clojure.test :refer :all]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.db :as db]
            [time-tracker.projects.test-helpers :as helpers]
            [time-tracker.users.test-helpers :as users.helpers]
            [time-tracker.auth.test-helpers :refer [fake-login-headers]]
            [time-tracker.auth.core :as auth]))

(use-fixtures :once fixtures/migrate-test-db fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)

(defn http-request
  ([method url google-id] (http-request method url google-id nil))
  ([method url google-id body]
   (let [params       (merge {:url url
                              :method method
                              :headers (merge (fake-login-headers google-id)
                                              {"Content-Type" "application/json"})
                              :as :text}
                             (if body {:body (json/encode body)}))
         response     @(http/request params)
         decoded-body (json/decode (:body response))]
     (assoc response :body decoded-body))))

(deftest retrieve-single-project
  (let [gen-projects (helpers/populate-data! {"gid1" ["foo"]})
        project-id   (get gen-projects "foo")
        url          (format "http://localhost:8000/projects/%s/" project-id)]

    (testing "Authorized"
      (let [{:keys [status body]} (http-request :get url "gid1")
            expected-body         {"id" project-id "name" "foo"}]
        (is (= status 200))
        (is (= expected-body body))))

    (testing "Unauthorized"
      (let [{:keys [status]} (http-request :get url "gid2")]
        (is (= status 403))))))


(deftest update-single-project
  (let [gen-projects (helpers/populate-data! {"gid1" ["foo"]})
        project-id   (get gen-projects "foo")
        url          (format "http://localhost:8000/projects/%s/" project-id)]

    (testing "Authorized"
      (let [{:keys [status body]} (http-request :put url "gid1" {"name" "goo"})
            expected-body         {"id" project-id "name" "goo"}]
        (is (= status 200))
        (is (= expected-body body))))

    (testing "Unauthorized"
      (let [{:keys [status]} (http-request :put url "gid2" {"name" "bar"})]
        (is (= status 403))))))


(deftest delete-single-project
  (let [gen-projects (helpers/populate-data! {"gid1" ["foo" "goo"]})
        format-url   "http://localhost:8000/projects/%s/"]

    (testing "Authorized"
      (let [project-id       (get gen-projects "foo")
            url              (format format-url project-id)
            {:keys [status]} (http-request :delete url "gid1")]
        (is (= 204 status))))

    (testing "Unauthorized"
      (let [project-id (get gen-projects "goo")
            url (format format-url project-id)
            {:keys [status]} (http-request :delete url "gid2")]
        (is (= 403 status))))))


(deftest retrieve-all-authorized-projects
  (let [gen-projects (helpers/populate-data! {"gid1" ["foo" "goo"]
                                              "gid2" ["bar" "baz"]})
        url          "http://localhost:8000/projects/"]

    (testing "User 1"
      (let [{:keys [status body]} (http-request :get url "gid1")
            project-names (map #(get % "name") body)]
        (is (= 200 status))
        (is (= (sort ["foo" "goo"])
               (sort project-names)))))

    (testing "User 2"
      (let [{:keys [status body]} (http-request :get url "gid2")
            project-names (map #(get % "name") body)]
        (is (= 200 status))
        (is (= (sort ["bar" "baz"])
               (sort project-names)))))))


(deftest create-project
  (users.helpers/create-users! ["Sai Abdul" "gid1" "admin"]
                               ["Paul Graham" "gid2" "user"])

  (let [url "http://localhost:8000/projects/"]
    (testing "Admin user"
      (let [{:keys [status body]} (http-request :post url "gid1"
                                                {"name" "foo"})]
        (is (= 201 status))
        (is (= "foo"
               (get body "name")))))))
