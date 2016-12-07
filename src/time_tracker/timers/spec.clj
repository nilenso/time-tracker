(ns time-tracker.timers.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec]))

(s/def ::id number?)
(s/def ::epoch :core/positive-num)

(s/def ::hours :core/positive-num)
(s/def ::minutes :core/positive-num)
(s/def ::seconds :core/positive-num)

(s/def :timers.db/duration ::epoch)

(s/def ::timer-id ::id)
(s/def ::project-id ::id)
(s/def ::started-time ::epoch)
(s/def ::stop-time ::epoch)
(s/def ::current-time ::epoch)

(s/def :timers.pubsub/start-timer-args
  (s/keys :req-un [::timer-id ::started-time]))

(s/def :timers.pubsub/stop-timer-args
  (s/keys :req-un [::timer-id ::stop-time]))

(s/def :timers.pubsub/delete-timer-args
  (s/keys :req-un [::timer-id]))

(s/def :timers.pubsub/create-and-start-timer-args
  (s/keys :req-un [::project-id ::started-time]))

(s/def ::duration :timers.db/duration)
(s/def :timers.pubsub/change-timer-duration-args
  (s/keys :req-un [::timer-id ::duration ::current-time]))
