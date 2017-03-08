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

(defn- coerce-rates-map
  [raw-rates-map]
  (try
    (->> (util/transform-keys raw-rates-map util/parse-int)
         (fmap util/round-to-two-places))
    (catch java.lang.NumberFormatException _
      (util/raise-validation-error
        {:failure "Coercion failed for generate-invoice"}))))

(defn- coerce-taxes-map
  [raw-taxes-map]
  (try
    (->> (util/transform-keys raw-taxes-map name)
         (fmap util/round-to-two-places)
         not-empty)
    (catch java.lang.NumberFormatException _
      (util/raise-validation-error
        {:failure "Coercion failed for generate-invoice"}))))

(defn- coerce-and-validate-generate-invoice-body
  [request-body]
  (let [coerced-body (-> request-body
                         (update :user-id->rate coerce-rates-map)
                         (update :tax-rates coerce-taxes-map)
                         (update :currency keyword))]
    (util/validate-spec coerced-body ::handlers-spec/generate-invoice-params)
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
        projects (get-client-projects connection client)
        users    (util/normalize-entities (users-db/retrieve-all connection))
        timers   (get-timers-to-invoice connection start end projects)]
    (if (or (empty? projects)
            (empty? users))
      web-util/error-not-found
      (pdf-invoice (merge validated-body
                          {:users  users
                           :timers timers})))))
