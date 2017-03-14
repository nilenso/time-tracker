(ns time-tracker.invoices.handlers
  (:require [ring.util.response :as res]
            [time-tracker.users.db :as users-db]
            [time-tracker.projects.db :as projects-db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.invoices.core :as invoices-core]
            [time-tracker.util :as util]
            [time-tracker.web.util :as web-util]
            [time-tracker.invoices.handlers.spec :as handlers-spec]
            [time-tracker.projects.core :as projects-core]
            [clojure.algo.generic.functor :refer [fmap]]
            [ring.util.io :as ring-io]))

;; Download invoice endpoint
;; POST /download/invoice/

(defn- get-client-projects
  [connection client]
  (->> (projects-db/retrieve-all connection)
       (filter #(= client (projects-core/client %)))
       (util/normalize-entities)))

(defn- get-timers-to-invoice
  [connection start end invoice-project?]
  (->> (timers-db/retrieve-between connection start end)
       (filter #(invoice-project? (:project-id %)))
       (util/normalize-entities)))

(defn- bigdecify-rates
  [raw-rates-vector]
  (map #(update % :rate  util/round-to-two-places) raw-rates-vector))

(defn- bigdecify-taxes
  "Converts tax-percentage to bigdec and returns nil if argument is nil."
  [raw-taxes-vector]
  (->> raw-taxes-vector
       (map #(update % :tax-percentage util/round-to-two-places))
       not-empty))

(defn- coerce-and-validate-generate-invoice-body
  "Coerces rates and tax percentages to bigdec and currency strings to keywords, and validates data."
  [request-body]
  ;; Validate the un-coerced data
  (util/validate-spec request-body ::handlers-spec/generate-invoice-params-raw)
  (let [coerced-body (-> request-body
                         (update :user-id->rate bigdecify-rates)
                         (update :tax-rates bigdecify-taxes)
                         (update :currency keyword))]
    (util/validate-spec coerced-body ::handlers-spec/generate-invoice-params-coerced)
    coerced-body))

(defn- id->name
  [normalized-entities]
  (fmap :name normalized-entities))

(defn pdf-invoice
  [{:keys [users] :as invoice-data}]
  (let [invoice       (invoices-core/invoice invoice-data)
        user-id->name (id->name users)
        pdf-stream    (ring-io/piped-input-stream
                        (partial invoices-core/printable-invoice->pdf
                                 (invoices-core/printable-invoice invoice
                                                                  user-id->name)))]
    (-> (res/response pdf-stream)
        (res/content-type "application/pdf"))))

(defn generate-invoice
  [{:keys [body]} connection]
  (let [{:keys [start end client] :as validated-body}
        (coerce-and-validate-generate-invoice-body body)
        projects     (get-client-projects connection client)
        users        (util/normalize-entities (users-db/retrieve-all connection))
        timers       (get-timers-to-invoice connection start end projects)
        invoice-data (merge validated-body
                            {:users  users
                             :timers timers})]
    (util/validate-spec invoice-data ::handlers-spec/invoice-data)
    (if (or (empty? projects)
            (empty? users))
      web-util/error-not-found
      (pdf-invoice invoice-data))))
