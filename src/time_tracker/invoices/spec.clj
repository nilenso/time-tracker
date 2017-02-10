(ns time-tracker.invoices.spec
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [time-tracker.users.spec :as users-spec]
            [time-tracker.spec :as core-spec]))

(s/def ::hours (core-spec/positive-bigdec 2))
(s/def ::rate ::core-spec/money-val)

(s/def ::user-id->rate (s/map-of ::users-spec/id ::rate))

(s/def ::user-hours-row
  (s/keys :req-un [::users-spec/id ::hours ::rate]))

(defn- user-hours-row-from-id [id]
  (-> (s/gen ::user-hours-row)
      (gen/generate)
      (assoc :id id)))

(defn- user-hours-gen []
  (->> (gen/vector-distinct (s/gen ::users-spec/id))
       (gen/fmap #(map user-hours-row-from-id %))))

(defn- user-hours-pred
  [user-hours]
  (let [id-list    (mapv :id user-hours)
        unique-ids (set id-list)]
    (= (count id-list) (count unique-ids))))

(s/def ::user-hours (s/with-gen (s/and (s/coll-of ::user-hours-row :min-count 1)
                                       user-hours-pred)
                                user-hours-gen))

(s/def ::client ::core-spec/non-empty-string)
(s/def ::address ::core-spec/non-empty-string)
(s/def ::notes ::core-spec/non-empty-string)

(s/def ::tax-percentage (core-spec/positive-bigdec 2 0.01M 99.99M))

(s/def ::tax-rates (s/nilable (s/and (s/map-of ::core-spec/non-empty-string
                                               ::tax-percentage)
                                     #(>= (count %) 1))))

(s/def ::currency #{:usd :inr})


(def seconds-in-day (* 60 60 24))
(s/def ::date-range (s/and ::core-spec/epoch-range
                           (fn [{:keys [start end]}]
                             (<= seconds-in-day (- end start)))))

(s/def ::invoice
  (s/merge (s/keys :req-un [::client ::address ::tax-rates ::notes ::user-hours ::currency ::core-spec/utc-offset])
           ::date-range))

(s/def ::from-date ::core-spec/epoch)
(s/def ::to-date ::core-spec/epoch)
(s/def ::subtotal ::core-spec/money-val)
(s/def ::amount-due ::core-spec/money-val)

(s/def ::amount ::core-spec/money-val)

(s/def ::item
  (s/keys :req-un [::users-spec/name ::hours ::rate ::amount]))
(s/def ::items
  (s/coll-of ::item :min-count 1))

(s/def ::name ::core-spec/non-empty-string)
(s/def ::percentage (core-spec/positive-bigdec 2))
(s/def ::tax-amount-map
  (s/keys :req-un [::name ::amount ::percentage]))
(s/def ::tax-amounts (s/coll-of ::tax-amount-map))

(s/def ::printable-invoice
  (s/keys :req-un [::client ::address ::currency ::notes ::from-date ::to-date
                   ::items ::subtotal ::tax-amounts ::amount-due ::core-spec/utc-offset]))