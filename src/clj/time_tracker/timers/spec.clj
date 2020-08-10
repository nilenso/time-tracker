(ns time-tracker.timers.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec :as core-spec]
            [time-tracker.util :as util]))

(s/def ::id ::core-spec/id)
(s/def ::task-id ::core-spec/id)
(s/def ::app-user-id ::id)
(s/def ::started-time-not-nilable (s/and ::core-spec/epoch
                                         #(<= % (util/current-epoch-seconds))))
(s/def ::started-time (s/nilable ::started-time))
(s/def ::duration ::core-spec/epoch)
(s/def ::time-created ::core-spec/epoch)
(s/def ::notes string?)

(s/def ::timer
  (s/keys :req-un [::id ::task-id ::app-user-id ::started-time ::duration ::time-created ::notes]))

(defn normalized-timers-spec
  [min-count]
  (core-spec/normalized-entities-spec ::timer min-count))
