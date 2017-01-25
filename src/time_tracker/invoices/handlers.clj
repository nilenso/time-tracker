(ns time-tracker.invoices.handlers
  (:require [ring.util.response :as res]
            [time-tracker.users.db :as users-db]
            [time-tracker.projects.db :as projects-db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.invoices.core :as invoices-core]
            [time-tracker.util :as util]
            [time-tracker.web.util :as web-util]
            [time-tracker.invoices.handlers.spec :as handlers-spec]
            [clojure.string :as string]))

;; Download invoice endpoint
;; /download/invoice/?start=<some-epoch>&end=<some-other-epoch>

(defn- coerce-and-validate-generate-invoice-params
  [params]
  (let [coerced-params (web-util/coerce-epoch-range-params params)]
    (util/validate-spec coerced-params ::handlers-spec/generate-invoice-params)
    coerced-params))

(defn- client-is?
  [project client]
  (= client (-> (string/split (:name project) #"\|" 2)
                (first))))

(defn- get-client-projects
  [connection client]
  (->> (projects-db/retrieve-all connection)
       (filter #(client-is? % client))
       (util/normalize-entities)))

(defn- get-timers-to-invoice
  [connection start end projects]
  (->> (timers-db/retrieve-between connection start end)
       (filter #(projects (:project-id %)))
       (util/normalize-entities)))

(defn generate-invoice
  [{:keys [params]} connection]
  (let [{:keys [start end client]} (coerce-and-validate-generate-invoice-params params)
        projects                   (get-client-projects connection client)]
    (if (empty? projects)
      web-util/error-not-found
      (let [users      (util/normalize-entities (users-db/retrieve-all connection))
            timers     (get-timers-to-invoice connection start end projects)
            csv-string (invoices-core/generate-csv users projects timers)]
        (res/response csv-string)))))
