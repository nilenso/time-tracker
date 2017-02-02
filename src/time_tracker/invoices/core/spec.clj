(ns time-tracker.invoices.core.spec
  (:require [time-tracker.invoices.core :as invoices-core]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [time-tracker.spec :as core-spec]
            [time-tracker.timers.spec :as timers-spec]
            [time-tracker.users.spec :as users-spec]
            [time-tracker.projects.spec :as projects-spec]
            [time-tracker.util :as util]))

(s/def ::user-id->hours
  (s/map-of ::core-spec/id ::core-spec/positive-num))

(defn normalized-pred
  [entity-map]
  (every? (fn [[k v]] (= k (:id v))) entity-map))

(defn normalized-entities-spec
  [entity-spec]
  (s/with-gen (s/and (s/map-of ::core-spec/id entity-spec :min-count 1)
                     normalized-pred)
    (fn [] (gen/fmap util/normalize-entities
                     (gen/list (s/gen entity-spec))))))

(s/def ::users (normalized-entities-spec ::users-spec/user))
(s/def ::timers (normalized-entities-spec ::timers-spec/timer))

(s/def ::add-hours-args (s/cat :user-id->hours ::user-id->hours
                             :timer ::timers-spec/timer))

(defn add-hours-args-gen []
  (gen/one-of [(gen/bind
                 ;; user-id and hours
                 (gen/tuple (s/gen ::core-spec/positive-int) (s/gen ::core-spec/positive-num))
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

(s/fdef build-user-id->hours
        :args (s/with-gen (s/cat :required-user-ids (s/coll-of ::core-spec/id)
                                 :timers            ::timers)
                build-user-id->hours-args-gen)
        :ret ::user-id->hours)

(s/def ::id->name-map
  (s/map-of ::core-spec/id string?))

(defn- csv-rows-ret
  [ret]
  (let [names        (mapv first ret)
        unique-names (set names)]
    ;; The names must be unique.
    (= (count names) (count unique-names))))

(defn- csv-rows-args-gen []
  (gen/bind (users-timers-gen)
            (fn [[users timers]]
              (gen/tuple
               (gen/return (invoices-core/build-user-id->hours (keys users) timers))
               (gen/return (invoices-core/id->name users))))))

(s/fdef invoices-core/csv-rows
        :args (s/with-gen (s/cat :user-id->hours ::user-id->hours
                                 :user-id->name ::id->name-map)
                csv-rows-args-gen)
        :ret (s/and (s/coll-of
                     (s/cat :user-name string?
                            :hours ::core-spec/positive-num))
                    list?
                    csv-rows-ret))

(s/fdef invoices-core/generate-csv
        :args (s/with-gen (s/cat :users ::users
                                 :timers ::timers)
                users-timers-gen)
        :ret (s/spec string?))
