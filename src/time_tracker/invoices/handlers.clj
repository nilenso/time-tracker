(ns time-tracker.invoices.handlers
  (:require [ring.util.response :as res]
            [time-tracker.users.db :as users-db]
            [time-tracker.projects.db :as projects-db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.invoices.core :as invoices-core]
            [clojure.algo.generic.functor :refer [fmap]]
            [time-tracker.util :as util]
            [time-tracker.web.util :as web-util]))

;; Download invoice endpoint
;; /download/invoice/?start=<some-epoch>&end=<some-other-epoch>

(defn generate-invoice
  [request connection]
  (let [{:keys [start end]} (web-util/coerce-and-validate-epoch-range (:params request))
        users               (util/normalize-entities (users-db/retrieve-all connection))
        projects            (util/normalize-entities (projects-db/retrieve-all connection))
        timers              (util/normalize-entities (timers-db/retrieve-between connection start end))
        csv-string          (invoices-core/generate-csv users projects timers)]
    (res/response csv-string)))
