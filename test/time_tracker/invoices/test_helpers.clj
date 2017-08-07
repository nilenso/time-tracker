(ns time-tracker.invoices.test-helpers
  (:require [clojure.java.jdbc :as jdbc]
            [time-tracker.db :as db]
            [time-tracker.invoices.db :as invoices-db]
            [yesql.core :refer [defqueries]]
            [time-tracker.invoices.db :as invoices-db]))

(defn create-invoice!
  "Creates a single invoice in test database"
  [invoice-data]
  (jdbc/with-db-transaction [conn (db/connection)]
    (:id (invoices-db/create! conn invoice-data))))
