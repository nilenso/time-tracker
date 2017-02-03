(ns time-tracker.invoices.core-test
  (:require [time-tracker.invoices.core :as invoices-core]
            [time-tracker.invoices.core.spec]
            [clojure.test :refer :all]
            [time-tracker.test-helpers :refer [assert-generative-test]]))

(deftest add-hours-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/add-hours {:max-size 30})))

(deftest build-user-id->hours-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/build-user-id->hours {:max-size 30})))

(deftest csv-rows-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/csv-rows {:max-size 30})))
