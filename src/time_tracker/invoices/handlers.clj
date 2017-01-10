(ns time-tracker.invoices.handlers
  (:require [ring.util.response :as res]
            [time-tracker.users.db :as users-db]
            [time-tracker.projects.db :as projects-db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.invoices.core :as invoices-core]
            [time-tracker.util :as util]))

;; Download invoice endpoint
;; /download/invoice/

(defn generate-invoice
  [request connection]
  (let [users      (util/normalize-entities (users-db/retrieve-all connection))
        projects   (util/normalize-entities (projects-db/retrieve-all connection))
        timers     (util/normalize-entities (timers-db/retrieve-all connection))
        csv-string (invoices-core/generate-csv users projects timers)]
    ;; TODO: add current date in the invoice filename
    (-> (res/response csv-string)
        (res/header "Content-Disposition" "attachment; filename=\"invoice.csv\""))))
