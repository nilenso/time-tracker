(ns time-tracker.invoices.core
  (:require [time-tracker.timers.core :as timers-core]
            [clojure.algo.generic.functor :refer [fmap]]
            [time-tracker.util :as util]
            [clj-pdf.core :as clj-pdf]
            [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clojure.spec :as s]
            [time-tracker.invoices.spec :as invoices-spec]
            [time-tracker.spec :as core-spec]))

(defn- seconds->hours
  [seconds]
  (double (/ seconds 3600)))

(defn add-hours
  "Adds the hours logged against the timer entry
  to the `user-id->hours` map."
  [user-id->hours {:keys [app-user-id] :as timer}]
  (let [hours-to-add  (-> (timers-core/elapsed-time timer)
                          (seconds->hours)
                          (util/round-to-two-places))
        current-hours (get user-id->hours app-user-id)
        new-hours     (+ (or current-hours 0.00M) hours-to-add)]
    (assoc user-id->hours app-user-id new-hours)))

(defn build-user-id->hours
  "Returns a map of {user-id hours-logged}"
  [required-user-ids timers]
  (let [user-id->zero (zipmap required-user-ids (repeat 0.00M))]
    (reduce add-hours user-id->zero (vals timers))))

(defn- user-hours
  "Generates a seq of {:id :rate :hours}.
  There will be a row for every user in `users`."
  [users timers user-id->rate-vector]
  (let [user-ids       (keys users)
        user-id->hours (build-user-id->hours user-ids timers)
        user-id->rate (into {} (map #(hash-map (:user-id %) (:rate %)) user-id->rate-vector))]
    (for [user-id user-ids]
      {:id    user-id
       :rate  (user-id->rate user-id)
       :hours (user-id->hours user-id)})))

(defn invoice
  "Constructs an invoice"
  [{:keys [users timers user-id->rate] :as invoice-params}]
  {:pre  [(seq users)]
   :post [(s/valid? ::invoices-spec/invoice %)]}
  (merge (select-keys invoice-params [:start :end :client :address
                                      :notes :tax-rates :currency :utc-offset])
         {:user-hours (user-hours users timers user-id->rate)}))

;; TODO: Map these to their symbols and render ₹ properly
(def currency-symbols
  {:usd "USD "
   :inr "INR "})

(defn- money-str
  [currency amount]
  (str (currency-symbols currency) amount))

(defn- invoice-items
  [{:keys [user-hours currency]} user-id->name]
  (for [{:keys [id hours rate]} user-hours]
    {:name   (user-id->name id)
     :hours  hours
     :rate   rate
     :amount (util/round-to-two-places (* hours rate))}))

(defn- subtotal
  [invoice-items]
  {:pre  [(s/valid? ::invoices-spec/items invoice-items)]
   :post [(s/valid? ::core-spec/money-val %)]}
  (->> invoice-items
       (map :amount)
       (apply +)))

(defn- tax-amounts
  [taxes subtotal-amount]
  (for [{:keys [tax-name tax-percentage]} taxes]
    {:name       tax-name
     :amount     (-> (* subtotal-amount (util/divide-money tax-percentage 100.00M))
                     (util/round-to-two-places))
     :percentage tax-percentage}))

(defn- grand-total
  [subtotal-amount tax-maps]
  {:pre  [(s/valid? ::core-spec/money-val subtotal-amount)
          (s/valid? ::invoices-spec/tax-amounts tax-maps)]
   :post [(s/valid? ::core-spec/money-val %)]}
  (-> (apply + subtotal-amount (map :amount tax-maps))))

(defn printable-invoice
  [{:keys [tax-rates start end utc-offset] :as invoice} user-id->name]
  (let [items           (invoice-items invoice user-id->name)
        subtotal-amount (subtotal items)
        taxes           (tax-amounts tax-rates subtotal-amount)
        amount-due      (grand-total subtotal-amount taxes)
        from-date       start
        ;; `end` is exclusive, but the to-date must be inclusive.
        to-date         (-> (util/epoch->clj-time end utc-offset)
                            (time/minus (time/days 1))
                            (util/to-epoch-seconds))]
    (def *gt-output amount-due)
    (merge (select-keys invoice [:client :address :currency :utc-offset :notes])
           {:items items
            :subtotal subtotal-amount
            :amount-due amount-due
            :from-date from-date
            :to-date to-date
            :tax-amounts taxes})))

(defn- format-tax-amount-map
  [{:keys [name amount percentage]} currency]
  (str name " (" percentage "%): " (money-str currency amount)))

(defn- date->string [date-epoch utc-offset]
  (time-format/unparse (time-format/formatter
                         "dd-MM-yyyy"
                         (util/utc-offset->clj-timezone utc-offset))
                       (util/epoch->clj-time date-epoch utc-offset)))

(defn printable-invoice->pdf
  "Generates a PDF from a printable invoice."
  [{:keys [utc-offset currency] :as printable-invoice} out]
  {:pre [(seq (:items printable-invoice))]}
  (let [money (partial money-str currency)]
    (clj-pdf/pdf
      [{:font {:encoding :unicode}}
       [:paragraph "Invoice for " (date->string (:from-date printable-invoice)
                                                utc-offset)
        " to " (date->string (:to-date printable-invoice) utc-offset)]
       [:paragraph "Client: " (:client printable-invoice)]
       [:phrase "Address:"]
       [:paragraph (:address printable-invoice)]
       (into [:table
              {:header ["Name" "Quantity" "Unit Price" "Amount"]}]
             (for [{:keys [name rate hours amount]} (:items printable-invoice)]
               [name (str hours) (money rate) (money amount)]))
       [:paragraph "Subtotal: " (money (:subtotal printable-invoice))]
       ;; clj-pdf throws an error when it encounters an empty sequence ಠ_ಠ
       (not-empty
         (for [tax-amount-map (:tax-amounts printable-invoice)]
           [:paragraph (format-tax-amount-map tax-amount-map currency)]))
       [:paragraph "Amount due: " (money (:amount-due printable-invoice))]
       [:phrase "Notes:"]
       [:paragraph (:notes printable-invoice)]]
      out)))
