(ns time-tracker.invoices.core.spec
  (:require [time-tracker.invoices.core :as invoices-core]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [time-tracker.invoices.spec :as invoices-spec]
            [time-tracker.spec :as core-spec]
            [time-tracker.timers.spec :as timers-spec]
            [time-tracker.users.spec :as users-spec]
            [time-tracker.util :as util]))

(s/def ::user-id->hours
  (s/map-of ::users-spec/id ::invoices-spec/hours))

(s/def ::users (users-spec/normalized-users-spec 1))
(s/def ::timers (timers-spec/normalized-timers-spec 0))

(s/def ::add-hours-args (s/cat :user-id->hours ::user-id->hours
                             :timer ::timers-spec/timer))

(defn add-hours-args-gen []
  (gen/one-of [(gen/bind
                 ;; user-id and hours
                 (gen/tuple (s/gen ::core-spec/positive-int)
                            (s/gen ::core-spec/positive-num))
                 (fn [[user-id hours]]
                   (gen/tuple
                    (gen/fmap #(assoc % user-id hours)
                              (s/gen ::user-id->hours))
                    (gen/fmap #(assoc % :app-user-id user-id)
                              (s/gen ::timers-spec/timer)))))
               (s/gen ::add-hours-args)]))

(defn- add-hours-pred
  [{:keys [args ret]}]
  (let [{:keys [user-id->hours timer]} args
        prev-hours (user-id->hours (:app-user-id timer))
        new-hours (get ret (:app-user-id timer))]
    (if (nil? prev-hours)
      (>= new-hours 0)
      (>= new-hours prev-hours))))

(s/fdef invoices-core/add-hours
        :args (s/with-gen ::add-hours-args add-hours-args-gen)
        :ret ::user-id->hours
        :fn add-hours-pred)

(defn timers-for-user-gen []
  (gen/bind
   (s/gen ::users-spec/user)
   (fn [user]
     (gen/tuple
      (gen/return user)
      (gen/list (gen/fmap #(assoc % :app-user-id (:id user))
                          (s/gen ::timers-spec/timer)))))))

(defn users-timers-gen []
  (gen/bind
   (gen/list (timers-for-user-gen))
   (fn [args-list]
     (gen/return (reduce (fn [[users-map timers-map]
                              [user timers]]
                           [(assoc users-map (:id user) user)
                            (merge timers-map (util/normalize-entities timers))])
                         [{} {}]
                         args-list)))))

(defn- build-user-id->hours-args-gen []
  (gen/bind (users-timers-gen)
            (fn [[users timers]]
              (gen/tuple
               (gen/return (keys users))
               (gen/return timers)))))

(defn- build-user-id->hours-pred
  [{:keys [args ret]}]
  ;; All required user ids must be present.
  (= (set (:required-user-ids args))
     (set (keys ret))))

(s/fdef invoices-core/build-user-id->hours
        :args (s/with-gen (s/cat :required-user-ids (s/coll-of ::core-spec/id)
                                 :timers            ::timers)
                build-user-id->hours-args-gen)
        :ret ::user-id->hours
        :fn build-user-id->hours-pred)

(defn- user-id->rate-gen [users]
  (gen/return
   (vec (for [[user-id user] users]
          {:user-id user-id :rate (gen/generate (s/gen ::invoices-spec/rate))}))))

(defn- user-amounts-args-gen []
  (gen/bind (users-timers-gen)
            (fn [[users timers]]
              (gen/tuple
                (gen/return users)
                (gen/return timers)
                (user-id->rate-gen users)))))

(defn- user-amounts-pred
  [{:keys [args ret]}]
  (let [unique-args-ids (->> (:users args)
                             (keys)
                             (set))
        unique-ret-ids (set (map :id ret))]
    ;; Every user passed should be present in the returned rows.
    (= unique-args-ids unique-ret-ids)))

(s/fdef invoices-core/user-hours
        :args (s/with-gen (s/cat :users ::users
                                 :timers ::timers
                                 :user-id->rate ::invoices-spec/user-id->rate)
                          user-amounts-args-gen)
        :ret ::invoices-spec/user-hours
        :fn user-amounts-pred)

(s/def ::user-id->name (s/fspec :args (s/cat :id ::users-spec/id)
                                :ret ::users-spec/name))

(s/fdef invoices-core/invoice-items
        :args (s/cat :invoice ::invoices-spec/invoice
                     :user-id->name ::user-id->name)
        :ret ::invoices-spec/items)

(s/fdef invoices-core/subtotal
        :args (s/cat :invoice-items ::invoices-spec/items)
        :ret ::core-spec/money-val)

(defn- tax-amounts-pred
  [{:keys [args ret]}]
  (= (into {} (map (juxt :tax-name :tax-percentage) (:tax-rates args)))
     (zipmap  (map :name ret)
              (map :percentage ret))))

(s/fdef invoices-core/tax-amounts
        :args (s/cat :tax-rates ::invoices-spec/tax-rates
                     :subtotal-amount ::core-spec/money-val)
        :ret ::invoices-spec/tax-amounts
        :fn tax-amounts-pred)

(s/fdef invoices-core/grand-total
        :args (s/cat :subtotal-amount ::core-spec/money-val
                     :tax-maps ::invoices-spec/tax-amounts)
        :ret ::core-spec/money-val)

(s/fdef invoices-core/printable-invoice
        :args (s/cat :invoice ::invoices-spec/invoice
                     :user-id->name ::user-id->name)
        :ret ::invoices-spec/printable-invoice)
