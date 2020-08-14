(ns time-tracker.data-import.harvest.import-test
  (:require [time-tracker.data-import.harvest.import :as harvest-import]
            [time-tracker.data-import.harvest.import.spec]
            [clojure.test :refer :all]
            [time-tracker.test-helpers :refer [assert-generative-test]]))

(comment deftest denormalize-data-test
  (testing "Generative test"
    (assert-generative-test `harvest-import/denormalize-data)))
