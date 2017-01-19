(ns time-tracker.invoices.handlers
  (:require [ring.util.response :as res]
            [time-tracker.users.db :as users-db]
            [time-tracker.projects.db :as projects-db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.invoices.core :as invoices-core]
            [clojure.algo.generic.functor :refer [fmap]]
            [time-tracker.util :as util]
            [time-tracker.invoices.handlers.spec :as handlers-spec]))

;; Download invoice endpoint
;; /download/invoice/?start=<some-epoch>&end=<some-other-epoch>

(defn- coerce-generate-invoice-params
  [params]
  (try
    (fmap #(Long/parseLong %) params)
    (catch Exception ex
      (throw (ex-info "Validation failed" {:event :validation-failed
                                           :params params})))))

(defn- coerce-and-validate-params
  [params]
  (let [coerced-params (coerce-generate-invoice-params params)]
    (util/validate-spec coerced-params ::handlers-spec/generate-invoice-params)
    coerced-params))

(defn generate-invoice
  [request connection]
  (let [{:keys [start end]} (coerce-and-validate-params (:params request))
        users               (util/normalize-entities (users-db/retrieve-all connection))
        projects            (util/normalize-entities (projects-db/retrieve-all connection))
        timers              (util/normalize-entities (timers-db/retrieve-between connection start end))
        csv-string          (invoices-core/generate-csv users projects timers)]
    (res/response csv-string)))
