(ns time-tracker.invoices.handlers.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec :as core-spec]
            [time-tracker.invoices.spec :as invoices-spec]))

(s/def ::generate-invoice-params
  (s/merge (s/keys :req-un [::invoices-spec/client
                            ::invoices-spec/address
                            ::invoices-spec/notes
                            ::invoices-spec/user-id->rate
                            ::invoices-spec/tax-rates
                            ::invoices-spec/currency
                            ::invoices-spec/utc-offset])
           ::invoices-spec/date-range))
