(ns time-tracker.invoices.core.spec
  (:require [time-tracker.invoices.core :as invoices-core]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [time-tracker.spec]
            [time-tracker.timers.spec]
            [time-tracker.users.spec]
            [time-tracker.projects.spec]
            [time-tracker.util :as util]))

(s/def ::time-map
  (s/map-of :core/id (s/map-of :core/id :core/positive-num)))

(defn normalized-pred
  [entity-map]
  (every? (fn [[k v]] (= k (:id v))) entity-map))

(defn normalized-entities-spec
  [entity-spec]
  (s/with-gen (s/and (s/map-of :core/id entity-spec :min-count 1)
                     normalized-pred)
    (fn [] (gen/fmap util/normalize-entities
                     (gen/list (s/gen entity-spec))))))

(s/def ::users (normalized-entities-spec :users.db/user))
(s/def ::projects (normalized-entities-spec :projects.db/project))
(s/def ::timers (normalized-entities-spec :timers.db/timer))

(defn- empty-time-map-pred
  [{:keys [args ret]}]
  (let [user-ids    (set (keys(:users args)))
        project-ids (set (keys(:projects args)))]
    (and (= user-ids
            (set (keys ret)))
         (every? #(= project-ids (set (keys %)))
                 (vals ret)))))

(s/fdef invoices-core/empty-time-map
        :args (s/cat :users ::users
                     :projects ::projects)
        :ret ::time-map
        :fn empty-time-map-pred)

(defn add-hours-args-gen []
  (gen/bind
   (gen/vector (s/gen :core/positive-int) 3)
   (fn [v]
     (gen/tuple
      (gen/fmap #(assoc-in % (take 2 v) (get v 2))
                (s/gen ::time-map))
      (gen/fmap #(merge % {:app-user-id (first v)
                           :project-id  (second v)})
                (s/gen :timers.db/timer))))))

(s/fdef invoices-core/add-hours
        :args (s/with-gen (s/cat :time-map ::time-map
                                 :timer :timers.db/timer)
                add-hours-args-gen)
        :ret ::time-map)

(defn timers-for-user-and-project-gen []
  (gen/bind
   (s/gen (s/cat :user    :users.db/user
                 :project :projects.db/project))
   (fn [[user project]]
     (gen/tuple
      (gen/return user)
      (gen/return project)
      (gen/list (gen/fmap #(merge % {:app-user-id (:id user) :project-id (:id project)})
                          (s/gen :timers.db/timer)))))))

(defn user-project-timer-gen []
  (gen/bind
   (gen/list (timers-for-user-and-project-gen))
   (fn [args-list]
     (gen/return (reduce (fn [[users-map project-map timers-map]
                              [user project timers]]
                           [(assoc users-map (:id user) user)
                            (assoc project-map (:id project) project)
                            (merge timers-map (util/normalize-entities timers))])
                         [{} {} {}]
                         args-list)))))

(s/fdef invoices-core/build-time-map
        :args (s/with-gen (s/cat :users ::users
                                 :projects ::projects
                                 :timers ::timers)
                user-project-timer-gen)
        :ret ::time-map)

(defn time-map->csv-rows-args-gen []
  (gen/bind (user-project-timer-gen)
            (fn [[users projects timers]]
              (gen/tuple
               (gen/return (invoices-core/build-time-map users projects timers))
               (gen/return users)
               (gen/return projects)
               (gen/return timers)))))

(s/fdef invoices-core/time-map->csv-rows
        :args (s/with-gen (s/cat :time-map ::time-map
                                 :users ::users
                                 :projects ::projects
                                 :timers ::timers)
                time-map->csv-rows-args-gen)
        :ret (s/coll-of
              (s/cat :user-name string?
                     :project-name string?
                     :hours :core/positive-num)))


(s/fdef invoices-core/generate-csv
        :args (s/cat :users ::users
                     :projects ::projects
                     :timers ::timers)
        :ret (s/spec string?))
