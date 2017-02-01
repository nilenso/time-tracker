(ns time-tracker.invoices.core-test
  (:require [time-tracker.invoices.core :as invoices-core]
            [time-tracker.invoices.core.spec]
            [clojure.test :refer :all]
            [time-tracker.test-helpers :refer [assert-generative-test]]))

(deftest empty-time-map-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/empty-time-map)))

(deftest add-hours-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/add-hours {:max-size 30})))

(deftest build-time-map-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/build-time-map {:max-size 30})))

(deftest time-map->csv-rows-test
  (testing "Generative test"
    (assert-generative-test `invoices-core/time-map->csv-rows {:max-size 30})))
