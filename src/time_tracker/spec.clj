(ns time-tracker.spec
  (:require [clojure.spec :as s]))

;; General useful specs

(s/def :core/positive-num (s/and number? #(>= % 0)))
