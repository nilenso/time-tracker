(ns time-tracker.invoices.functional-test
  (:require [clojure.test :refer :all]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.test-helpers :as test-helpers]
            [time-tracker.projects.test-helpers :as projects-helpers]
            [time-tracker.users.test-helpers :as users-helpers]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.util :as util]
            [gniazdo.core :as ws]
            [cheshire.core :as json]
            [clojure.string :as string]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)


(deftest download-invoice-test
  (let [project-url           "http://localhost:8000/api/projects/"
        invoice-url           "http://localhost:8000/download/invoice/"
        _                     (users-helpers/create-users! ["sandy" "gid1" "admin"])
        {:keys [status body]} (test-helpers/http-request :post project-url "gid1"
                                                         {:name "foo"})
        project-id            (get body "id")
        current-time          (util/current-epoch-seconds)
        [ws-chan socket]      (test-helpers/make-ws-connection "gid1")]
    (try
      (ws/send-msg socket (json/encode
                           {:command "create-and-start-timer"
                            :project-id project-id
                            :started-time current-time
                            :created-time current-time
                            :notes "baz"}))
      (let [create-response (test-helpers/try-take!! ws-chan)
            start-response (test-helpers/try-take!! ws-chan)
            timer-id (:id create-response)]
        (ws/send-msg socket (json/encode
                             {:command "stop-timer"
                              :timer-id timer-id
                              :stop-time current-time}))
        (ws/send-msg socket (json/encode
                             {:command "update-timer"
                              :timer-id timer-id
                              :current-time current-time
                              :duration 3600
                              :notes "baz"}))
        (testing "valid args"
          (let [query-string          (str "?start=" (- current-time 10)
                                           "&end="   (+ current-time 10))
                {:keys [status body]} (test-helpers/http-request-raw :get
                                                                     (str invoice-url query-string)
                                                                     "gid1")]
            (is (= 200 status))
            (is (string/includes? body "foo"))
            (is (string/includes? body "sandy"))))

        (testing "invalid args"
          (let [query-string          (str "?start=" "foo"
                                           "&end="   (+ current-time 10))
                {:keys [status body]} (test-helpers/http-request-raw :get
                                                                     (str invoice-url query-string)
                                                                     "gid1")]
            (is (= 400 status)))))

      (finally (ws/close socket)))))
