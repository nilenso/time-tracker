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
            [clojure.string :as string]
            [clojure.string :as str]))

(use-fixtures :once fixtures/init! fixtures/migrate-test-db fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)

(defn- create-timer-over-ws
  [ws-chan socket project-id current-time duration]
  (ws/send-msg socket (json/encode
                       {:command "create-and-start-timer"
                        :project-id project-id
                        :started-time current-time
                        :created-time current-time
                        :notes ""}))

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
                          :notes "created for testing"}))
    (let [_               (test-helpers/try-take!! ws-chan)
          update-response (test-helpers/try-take!! ws-chan)]
      (dissoc update-response :type))))


(deftest download-invoice-test
  (let [project-url           (str/join [test-helpers/test-api "projects/"])
        invoice-url           (str/join [test-helpers/test-host "download/invoice/"])
        users-url             (str/join [test-helpers/test-api "users/"])
        _                     (users-helpers/create-users! ["sandy" "gid1" "admin"]
                                                           ["quux" "gid2" "admin"])
        _                     (test-helpers/http-request :post project-url "gid1"
                                                         {:name "bar|baz"})
        {users-body :body}    (test-helpers/http-request :get users-url "gid1")
        user-ids              (set (map #(get % "id") users-body))
        {:keys [status body]} (test-helpers/http-request :post project-url "gid1"
                                                         {:name "foo|goo"})
        project-id            (get body "id")
        current-time          (util/current-epoch-seconds)
        [ws-chan socket]      (test-helpers/make-ws-connection "gid1")
        created-timer         (create-timer-over-ws ws-chan socket
                                                    project-id current-time
                                                    3600)]
    (try
      (testing "Existing client"
        (let [data             {:client "foo"
                                :start (- current-time (* 60 60 24))
                                :end current-time
                                :address "baz"
                                :notes "quux"
                                :user-rates (vec (for [user-id user-ids]
                                                   {:user-id user-id
                                                    :rate    5}))
                                :utc-offset 330
                                :currency :inr
                                :tax-rates nil}
              {:keys [status]} (test-helpers/http-request-raw
                                 :post
                                 invoice-url
                                 "gid1"
                                 data)]
          (is (= 200 status))))

      (testing "Another client with no timers"
        (let [data                  {:client "bar"
                                     :start (- current-time (* 60 60 24))
                                     :end current-time
                                     :address "baz"
                                     :notes "quux"
                                     :user-rates (vec (for [user-id user-ids]
                                                   {:user-id user-id
                                                    :rate    5}))
                                     :utc-offset 330
                                     :currency :inr
                                     :tax-rates nil}
              {:keys [status body]} (test-helpers/http-request-raw
                                      :post
                                      invoice-url
                                      "gid1"
                                      data)]
          (is (= 200 status))))

      (testing "Client does not exist"
        (let [data                  {:client "quux"
                                     :start (- current-time (* 60 60 24))
                                     :end current-time
                                     :address "baz"
                                     :notes "quux"
                                     :user-rates (vec (for [user-id user-ids]
                                                   {:user-id user-id
                                                    :rate    5}))
                                     :utc-offset 330
                                     :currency :inr
                                     :tax-rates nil}
              {:keys [status body]} (test-helpers/http-request-raw
                                     :post
                                     invoice-url
                                     "gid1"
                                     data)]
          (is (= 404 status))))

      (testing "Invalid args"
        (let [data                  {:client "foo"
                                     :start (- current-time (* 60 60 24))
                                     :end current-time
                                     :address "baz"
                                     :notes "quux"
                                     :user-rates (vec (for [user-id user-ids]
                                                   {:user-id user-id
                                                    :rate    5}))
                                     :utc-offset "midget"
                                     :currency :inr
                                     :tax-rates nil}
              {:keys [status body]} (test-helpers/http-request-raw
                                      :post
                                      invoice-url
                                      "gid1"
                                      data)]
          (is (= 400 status))))

      (testing "Insufficient user rates"
        (let [user-id (first user-ids)
              data    {:client "foo"
                       :start (- current-time (* 60 60 24))
                       :end current-time
                       :address "baz"
                       :notes "quux"
                       :user-rates [{:user-id user-id
                                     :rate    3}]
                       :utc-offset 330
                       :currency :inr
                       :tax-rates nil}
              {:keys [status body]} (test-helpers/http-request-raw
                                      :post
                                      invoice-url
                                      "gid1"
                                      data)]
          (is (= 400 status))))

      (finally (ws/close socket)))))
