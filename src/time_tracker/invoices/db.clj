(ns time-tracker.invoices.db
  (:require [clojure.java.jdbc :as jdbc]))

(defn create!
  [connection invoice]
  (first (jdbc/insert! connection "invoice"
                       {"client" (:client invoice)
                        "address" (:address invoice)
                        "currency" (name (:currency invoice))
                        "utc_offset" (:utc-offset invoice)
                        "notes" (:notes invoice)
                        "items" (pr-str (:items invoice)),
                        "subtotal" (:subtotal invoice)
                        "amount_due" (:amount-due invoice)
                        "from_date" (:from-date invoice)
                        "to_date" (:to-date invoice)
                        "tax_amounts" (pr-str (:tax-amounts invoice)),
                        "paid" false})))
