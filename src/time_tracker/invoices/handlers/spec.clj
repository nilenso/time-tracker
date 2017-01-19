(ns time-tracker.invoices.handlers.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec :as core-spec]))

(s/def ::start ::core-spec/epoch)
(s/def ::end ::core-spec/epoch)

(s/def ::generate-invoice-params
  (s/and (s/keys :req-un [::start ::end])
         (fn [{:keys [start end]}]
           (< start end))))
