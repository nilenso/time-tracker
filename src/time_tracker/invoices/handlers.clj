(ns time-tracker.invoices.handlers
  (:require [ring.util.response :as res]
            [time-tracker.users.db :as users-db]
            [time-tracker.projects.db :as projects-db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.invoices.db :as invoices-db]
            [time-tracker.invoices.core :as invoices-core]
            [time-tracker.invoices.spec :as invoices-spec]
            [time-tracker.util :as util]
            [time-tracker.logging :as log]
            [time-tracker.web.util :as web-util]
            [time-tracker.invoices.handlers.spec :as handlers-spec]
            [time-tracker.projects.core :as projects-core]
            [clojure.algo.generic.functor :refer [fmap]]
            [ring.util.io :as ring-io]))

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
                         (update :user-rates bigdecify-rates)
                         (update :tax-rates bigdecify-taxes)
                         (update :currency keyword))]
    (util/validate-spec coerced-body ::handlers-spec/generate-invoice-params-coerced)
    coerced-body))

(defn- names-by-id
  [normalized-entities]
  (fmap :name normalized-entities))

(defn- printable-invoice
  [{:keys [users] :as invoice-data}]
  (let [invoice            (invoices-core/invoice invoice-data)]
    (invoices-core/printable-invoice invoice (names-by-id users))))

(defn- print-invoice
  [invoice-data]
  (let [printable-invoice  (printable-invoice invoice-data)
        pdf-stream         (ring-io/piped-input-stream
                            (partial invoices-core/generate-pdf printable-invoice))]
    (-> (res/response pdf-stream)
        (res/content-type "application/pdf"))))

;; Endpoint for saving and downloading an invoice
;; POST /api/invoices/
(defn create
  [{:keys [body]} connection]
  (let [{:keys [start end client] :as validated-body}
        (coerce-and-validate-generate-invoice-body body)
        projects     (get-client-projects connection client)
        timers       (get-timers-to-invoice connection start end projects)
        timer-users  (map :app-user-id (vals timers)) ;; coll of keys of users who are in the timers
        all-users    (util/normalize-entities (users-db/retrieve-all connection))
        users        (select-keys all-users timer-users)
        invoice-data (merge validated-body
                            {:users  users
                             :timers timers})]
    (util/validate-spec invoice-data ::handlers-spec/invoice-data)
    (if (or (empty? projects)
            (empty? users))
      web-util/error-not-found
      (do
        (invoices-db/create! connection
                             (printable-invoice invoice-data))
        (print-invoice invoice-data)))))

;; Endpoint for retrieving all invoices
;; GET /api/invoices/
(defn list-all
  "Retrieves all invoices"
  [request connection]
  (res/response (invoices-db/retrieve-all connection)))

;; Endpoint for retrieving a single invoice
;; GET /api/invoices/<id>/
(defn retrieve
  [request connection]
  (let [invoice-id (Integer/parseInt (:id (:route-params request)))]
    (if-let [invoice (invoices-db/retrieve
                      connection
                      invoice-id)]
      (do
        (log/debug {:event "invoice-received" :data invoice})
        (res/response invoice))
      web-util/error-not-found)))

;; Endpoint for updating a single invoice.
;; PUT /api/invoices/<id>
;; Calling this handler 'update' would shadow
;; clojure.core/update
(defn modify
  [{:keys [route-params body]} connection]
  ;; Validate input JSON should have only {paid: true} or {usable: false} but
  ;; not both as user can only update an unpaid invoice to paid or an usable
  ;; invoice to unusable.
  ;;
  ;; NOTE: Changing to unusable can only happen for an unpaid invoice. This is
  ;; currently handled by the frontend.
  (util/validate-spec body ::invoices-spec/invoice-update)
  (let [invoice-id (Integer/parseInt (:id route-params))
        [update-method update-value] (cond
                                       (contains? body :paid)
                                       [invoices-db/mark-invoice-paid! (select-keys body [:paid])]
                                       (contains? body :usable)
                                       [invoices-db/mark-invoice-unusable! (select-keys body [:usable])])]
    (if-let [updated-invoice (update-method
                               connection
                               invoice-id
                               update-value)]
      (res/response updated-invoice)
      web-util/error-not-found)))
