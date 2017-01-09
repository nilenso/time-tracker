(ns time-tracker.invoices.core-test
  (:require [time-tracker.invoices.core :as invoices-core]
            [clojure.test :refer :all]
            [clojure.spec.test :as stest]))

(deftest empty-time-map-test
  (testing "Generative test"
    (is (empty? (->> (stest/check `invoices-core/empty-time-map)
                     (map stest/abbrev-result)
                     (filter :failure)
                     (map :failure))))))

(deftest add-hours-test
  (testing "Generative test"
    (is (empty? (->> (stest/check `invoices-core/add-hours)
                     (map stest/abbrev-result)
                     (filter :failure)
                     (map :failure))))))

(deftest build-time-map-test
  (testing "Generative test"
    (is (empty? (->> (stest/check `invoices-core/build-time-map
                                  {:clojure.spec.test.check/opts {:num-tests 100}})
                     (map stest/abbrev-result)
                     (filter :failure)
                     (map :failure))))))

(deftest time-map->csv-rows-test
  (testing "Generative test"
    (is (empty? (->> (stest/check `invoices-core/time-map->csv-rows
                                  {:clojure.spec.test.check/opts {:num-tests 100}})
                     (map stest/abbrev-result)
                     (filter :failure)
                     (map :failure))))))
