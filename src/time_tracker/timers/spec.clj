(ns time-tracker.timers.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec :as core-spec]
            [time-tracker.util :as util]
            [time-tracker.timers.core :as timers-core]))

(def seconds-in-day (* 60 60 24))

(s/def ::epoch ::core-spec/positive-int)

(s/def ::id ::core-spec/id)
(s/def ::project-id ::core-spec/id)
(s/def ::app-user-id ::id)
(s/def ::started-time-not-nilable (s/and ::epoch
                                         #(<= % (util/current-epoch-seconds))))
(s/def ::started-time (s/nilable ::started-time))
(s/def ::duration (s/and ::core-spec/positive-int
                         #(<= % seconds-in-day)))
(s/def ::time-created ::epoch)
(s/def ::notes string?)

(s/def ::timer
  (s/and (s/keys :req-un [::id ::project-id ::app-user-id ::started-time ::duration ::time-created ::notes])
         #(<= (timers-core/elapsed-time %) seconds-in-day)))
