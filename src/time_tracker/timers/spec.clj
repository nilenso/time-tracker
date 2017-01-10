(ns time-tracker.timers.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec :as core-spec]
            [time-tracker.util :as util]))

(s/def ::epoch ::core-spec/positive-int)

(s/def ::id ::core-spec/id)
(s/def ::project-id ::core-spec/id)
(s/def ::app-user-id ::id)
(s/def ::started-time-not-nilable (s/and ::epoch
                                         #(<= % (util/current-epoch-seconds))))
(s/def ::started-time (s/nilable ::started-time))
(s/def ::duration ::epoch)
(s/def ::time-created ::epoch)

(s/def ::timer
  (s/keys :req-un [::id ::project-id ::app-user-id ::started-time ::duration ::time-created]))
