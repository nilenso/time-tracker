(ns time-tracker.invoices.core-test
  (:require [time-tracker.invoices.core :as invoices-core]
            [time-tracker.invoices.core.spec :as invoices-spec]
            [clojure.test :refer :all]
            [time-tracker.test-helpers :refer [assert-generative-test]]))

(deftest add-hours-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/add-hours {:max-size 30})))

(deftest build-user-id->hours-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/build-user-id->hours {:max-size 30})))

(deftest user-hours-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/user-hours {:max-size 30})))

(deftest invoice-items-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/invoice-items)))

(deftest subtotal-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/subtotal)))

(deftest tax-amounts-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/tax-amounts)))

(deftest grand-total-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/grand-total)))

(deftest printable-invoice-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/printable-invoice)))