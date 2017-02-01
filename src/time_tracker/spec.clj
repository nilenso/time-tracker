(ns time-tracker.spec
  (:require [clojure.spec :as s]))

;; General useful specs

(s/def ::positive-num (s/and number? #(>= % 0)))
(s/def ::positive-int (s/and int? #(>= % 0)))
(s/def ::id ::positive-int)
(s/def ::epoch ::positive-int)
