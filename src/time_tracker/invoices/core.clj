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
  [users timers user-rates]
  (let [user-ids     (keys users)
        hours-by-id  (build-user-id->hours user-ids timers)
        rates-by-id  (into {} (map (juxt :user-id :rate) user-rates))]
    (for [user-id user-ids]
      {:id    user-id
       :rate  (rates-by-id user-id)
       :hours (hours-by-id user-id)})))

(defn invoice
  "Constructs an invoice"
  [{:keys [users timers user-rates] :as invoice-params}]
  {:pre  [(seq users)]
   :post [(s/valid? ::invoices-spec/invoice %)]}
  (merge (select-keys invoice-params [:start :end :client :address
                                      :notes :tax-rates :currency :utc-offset])
         {:user-hours (user-hours users timers user-rates)}))

;; TODO: Map these to their symbols and render ₹ properly
(def currency-symbols
  {:usd "USD"
   :inr "INR"})

(defn- money-str
  [currency amount]
  (str (currency-symbols currency) " " amount))

(defn- invoice-items
  [{:keys [user-hours currency]} names-by-id]
  (for [{:keys [id hours rate]} user-hours]
    {:name   (names-by-id id)
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
  [{:keys [tax-rates start end utc-offset] :as invoice} names-by-id]
  (let [items           (invoice-items invoice names-by-id)
        subtotal-amount (subtotal items)
        taxes           (tax-amounts tax-rates subtotal-amount)
        amount-due      (grand-total subtotal-amount taxes)
        from-date       start
        ;; `end` is exclusive, but the to-date must be inclusive.
        to-date         (-> (util/epoch->clj-time end utc-offset)
                            (time/minus (time/days 1))
                            (util/to-epoch-seconds))]
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

(defn generate-pdf
  "Generates a PDF from a printable invoice."
  [{:keys [utc-offset currency] :as printable-invoice} out]
  {:pre [(seq (:items printable-invoice))]}
  (let [money (partial money-str currency)
        name (util/from-config :name)
        [address1 address2 address3] (clojure.string/split
                                      (util/from-config :address)
                                      #"\n")
        [client-address1 client-address2 & client-rest] (clojure.string/split
                                                         (:address printable-invoice)
                                                         #"\n")]
    (clj-pdf/pdf
     [{:font {:encoding :unicode}}
      [:image {:xscale 0.5 :yscale 0.5}
       (.getFile (clojure.java.io/resource (util/from-config :logo)))]

      ;; Nilenso + Client information
      [:table {:width 100 :border false :cell-border false :spacing -5 :num-cols 2}
       [[:cell {:align "left" :style :bold} name]
        [:cell {:align "right":style :bold} (str "Client: " (:client printable-invoice))]]
       [[:cell {:align "left"} address1]
        [:cell {:align "right"} client-address1]]
       [[:cell {:align "left"} address2]
        [:cell {:align "right"} client-address2]]
       [[:cell {:align "left"} address3]
        [:cell {:align "right"} (apply str client-rest)]]]

      [:paragraph {:spacing-before 50} [:chunk {:style :bold} "Issue Date: "]
       [:chunk (date->string (-> (time/now) (clj-time.coerce/to-long) (/ 1000))
                             utc-offset)]]
      [:paragraph {:spacing-before 10} [:chunk {:style :bold} "Subject: "]
       [:chunk (str "Invoice for "
                    (:client printable-invoice)
                    " from "
                    (date->string (:from-date printable-invoice) utc-offset)
                    " to "
                    (date->string (:to-date printable-invoice) utc-offset))]]

      ;; Staffing + Rates
      (into [:table {:width 100 :border true :cell-border true :num-cols 5
                     :widths [10 45 15 15 15]
                     :header ["Sl No." "Description" "Quantity" "Price" "Amount"]}]
            (map-indexed (fn [idx {:keys [name rate hours amount]}]
                           (map (fn [val]
                                [:cell {:background-color (if (even? idx)
                                                            [221 221 221]
                                                            [255 255 255])} val])
                              [(str idx) name (str rate) (str hours) (str amount)]))
                         (:items printable-invoice)))

      ;; Total amount + Tax information
      (conj (into [:pdf-table {:width-percent 45 :horizontal-align :right :border false}
                   [67 33]
                   [[:chunk {:style :bold} "Total"] (str (:subtotal printable-invoice))]]
                  (for [{:keys [name percentage amount]} (:tax-amounts printable-invoice)]
                    [(str name "(" (str percentage) "%): ") (str amount)]))
            [[:chunk {:style :bold} "Total including Taxes: "]
             (money (:amount-due printable-invoice))])

      ;; Notes
      [:paragraph {:spacing-before 60 :style :bold} "Notes:"]
      [:paragraph (:notes printable-invoice)]]
     out)))
