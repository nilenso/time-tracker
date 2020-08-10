(ns time-tracker.invoices.handlers.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec :as core-spec]
            [time-tracker.invoices.spec :as invoices-spec]
            [time-tracker.users.spec :as users-spec]
            [time-tracker.timers.spec :as timers-spec]))

(s/def ::rate number?)
(s/def ::user-rate-map (s/keys :req-un [::invoices-spec/user-id ::rate]))
(s/def ::user-rates (s/coll-of ::user-rate-map))

(s/def ::tax-percentage number?)
(s/def ::tax-rate-map-raw (s/keys :req-un [::invoices-spec/tax-name ::tax-percentage]))
(s/def ::tax-rates (s/nilable (s/coll-of ::tax-rate-map-raw)))

(s/def ::currency string?)

(s/def ::generate-invoice-params-raw
  (s/merge (s/keys :req-un [::invoices-spec/client
                            ::invoices-spec/address
                            ::invoices-spec/notes
                            ::user-rates
                            ::tax-rates
                            ::currency
                            ::core-spec/utc-offset])
           ::invoices-spec/date-range))

(s/def ::generate-invoice-params-coerced
  (s/merge (s/keys :req-un [::invoices-spec/client
                            ::invoices-spec/address
                            ::invoices-spec/notes
                            ::invoices-spec/user-rates
                            ::invoices-spec/tax-rates
                            ::invoices-spec/currency
                            ::core-spec/utc-offset])
           ::invoices-spec/date-range))

(s/def ::users (users-spec/normalized-users-spec 1))
(s/def ::timers (timers-spec/normalized-timers-spec 0))

(s/def ::invoice-data
  (s/merge ::generate-invoice-params-coerced
           (s/keys :req-un [::users-spec/users ::timers-spec/timers])))
