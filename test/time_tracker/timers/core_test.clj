(ns time-tracker.timers.core-test
  (:require [clojure.test :refer :all]
            [time-tracker.util :as util]
            [time-tracker.timers.core :as timers-core]
            [time-tracker.test-helpers :refer [assert-generative-test]]))

(deftest same-day-test
  (let [utc-offset (+ 30 (* 5 60))]
    (testing "Two epochs on the same day"
      (let [current-time  (util/current-epoch-seconds)
            some-time-ago (- current-time 150)]
        (is (timers-core/same-day? some-time-ago current-time utc-offset))
        (is (timers-core/same-day? current-time some-time-ago utc-offset))))

    (testing "Two epochs on different days"
      (let [seconds-in-day (* 60 60 24)
            current-time   (util/current-epoch-seconds)
            tomorrow       (+ current-time seconds-in-day)]
        (is (not (timers-core/same-day? tomorrow current-time utc-offset)))
        (is (not (timers-core/same-day? current-time tomorrow utc-offset)))))))

(deftest clamp-test
  (testing "Generative test"
    (assert-generative-test `timers-core/clamp)))
