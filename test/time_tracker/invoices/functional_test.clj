(ns time-tracker.invoices.functional-test
  (:require [clojure.test :refer :all]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.test-helpers :as test-helpers]
            [time-tracker.invoices.test-helpers :as invoices-helpers]
            [time-tracker.projects.test-helpers :as projects-helpers]
            [time-tracker.users.test-helpers :as users-helpers]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.util :as util]
            [gniazdo.core :as ws]
            [cheshire.core :as json]
            [clojure.string :as s]))

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

(deftest create-and-download-invoice-test
  (let [project-url           (s/join [(test-helpers/settings :api-root) "projects/"])
        invoice-url           (s/join [(test-helpers/settings :api-root) "invoices/"])
        users-url             (s/join [(test-helpers/settings :api-root) "users/"])
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
                                :end (+ current-time (* 60 60 24))
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
                                     :end (+ current-time (* 60 60 24))
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

      (finally (ws/close socket)))))

(deftest update-invoice-paid-test
  (let [invoices-url           (s/join [(test-helpers/settings :api-root) "invoices/"])
        unpaid-invoice-data    {:client       "MSF"
                                :address      "Via Delorosa"
                                :currency     "BTC"
                                :utc_offset   0
                                :notes        "A minimal unpaid Test invoice"
                                :items        nil
                                :subtotal     0.0
                                :amount-due   11.0
                                :from_date    0
                                :to_date      0
                                :tax-amounts  nil
                                :paid         false}
        unpaid-invoice-id      (invoices-helpers/create-invoice! unpaid-invoice-data)
        unpaid-invoice-url     (str invoices-url unpaid-invoice-id "/")
        paid-invoice-data      {:client       "PIH"
                                :address      "Rue Morgue"
                                :currency     "ETH"
                                :utc_offset   0
                                :notes        "A minimal paid Test invoice"
                                :items        nil
                                :subtotal     5.0
                                :amount-due   9.0
                                :from_date    0
                                :to_date      0
                                :tax-amounts  nil
                                :paid         true}
        paid-invoice-id        (invoices-helpers/create-invoice! paid-invoice-data)
        paid-invoice-url       (str invoices-url paid-invoice-id "/")]
    (try
      (testing "Marking an unpaid invoice as paid"
        (let [data             {:paid true}
              {:keys [status]} (test-helpers/http-request-raw
                                :put
                                unpaid-invoice-url
                                "gid1"
                                data)]
          (is (= 200 status))))

      (testing "Marking a paid invoice as unpaid should fail"
        (let [data                  {:paid false}
              {:keys [status body]} (test-helpers/http-request-raw
                                     :put
                                     paid-invoice-url
                                     "gid1"
                                     data)]
          (is (= 400 status)))))))


(deftest update-invoice-unusable-test
  (let [invoices-url           (s/join [(test-helpers/settings :api-root) "invoices/"])
        unusable-invoice-data    {:client       "MSF"
                                  :address      "Via Delorosa"
                                  :currency     "BTC"
                                  :utc_offset   0
                                  :notes        "A minimal unusable Test invoice"
                                  :items        nil
                                  :subtotal     0.0
                                  :amount-due   11.0
                                  :from_date    0
                                  :to_date      0
                                  :tax-amounts  nil
                                  :usable       false}
        unusable-invoice-id      (invoices-helpers/create-invoice! unusable-invoice-data)
        unusable-invoice-url     (str invoices-url unusable-invoice-id "/")
        usable-invoice-data      {:client       "PIH"
                                  :address      "Rue Morgue"
                                  :currency     "ETH"
                                  :utc_offset   0
                                  :notes        "A minimal usable Test invoice"
                                  :items        nil
                                  :subtotal     5.0
                                  :amount-due   9.0
                                  :from_date    0
                                  :to_date      0
                                  :tax-amounts  nil
                                  :usable       true}
        usable-invoice-id        (invoices-helpers/create-invoice! usable-invoice-data)
        usable-invoice-url       (str invoices-url usable-invoice-id "/")]
    (try
      (testing "Marking a usable invoice as unusable"
        (let [data                  {:usable false}
              {:keys [status body]} (test-helpers/http-request-raw
                                     :put
                                     usable-invoice-url
                                     "gid1"
                                     data)]
          (is (= 200 status))))

      (testing "Marking an unusable invoice as usable"
        (let [data             {:usable true}
              {:keys [status]} (test-helpers/http-request-raw
                                :put
                                unusable-invoice-url
                                "gid1"
                                data)]
          (is (= 400 status)))))))
