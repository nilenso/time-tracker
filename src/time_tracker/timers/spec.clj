(ns time-tracker.timers.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec]
            [time-tracker.util :as util]))

(s/def ::id :core/id)
(s/def ::epoch :core/positive-int)

(s/def ::hours :core/positive-num)
(s/def ::minutes :core/positive-num)
(s/def ::seconds :core/positive-num)

(s/def :timers.db/duration ::epoch)

(s/def ::timer-id ::id)
(s/def ::project-id ::id)
(s/def ::started-time (s/and ::epoch
                             #(<= % (util/current-epoch-seconds))))
(s/def ::stop-time ::epoch)
(s/def ::created-time ::epoch)
(s/def ::current-time ::epoch)
(s/def ::date ::epoch)
(s/def ::app-user-id ::id)
(s/def ::time-created ::epoch)
(s/def :timers.db/started-time (s/nilable ::started-time))

(s/def :timers.db/timer
  (s/keys :req-un [::id ::project-id ::app-user-id :timers.db/started-time ::duration ::time-created]))

(s/def :timers.pubsub/start-timer-args
  (s/keys :req-un [::timer-id ::started-time]))

(s/def :timers.pubsub/stop-timer-args
  (s/keys :req-un [::timer-id ::stop-time]))

(s/def :timers.pubsub/delete-timer-args
  (s/keys :req-un [::timer-id]))

(s/def :timers.pubsub/create-and-start-timer-args
  (s/keys :req-un [::project-id ::started-time ::created-time]))

(s/def ::duration :timers.db/duration)
(s/def :timers.pubsub/change-timer-duration-args
  (s/keys :req-un [::timer-id ::duration ::current-time]))

(s/def :timers.handlers/list-all-args
  (s/nilable (s/keys :req-un [::date])))
