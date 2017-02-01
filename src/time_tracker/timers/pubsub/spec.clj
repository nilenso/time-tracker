(ns time-tracker.timers.pubsub.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec :as core-spec]
            [time-tracker.timers.spec :as timers-spec]))

(s/def ::timer-id ::timers-spec/id)
(s/def ::started-time ::timers-spec/started-time-not-nilable)
(s/def ::created-time ::timers-spec/time-created)
(s/def ::current-time ::core-spec/epoch)
(s/def ::stop-time ::core-spec/epoch)
(s/def ::notes ::timers-spec/notes)

(s/def ::start-timer-args
  (s/keys :req-un [::timer-id ::started-time]))

(s/def ::stop-timer-args
  (s/keys :req-un [::timer-id ::stop-time]))

(s/def ::delete-timer-args
  (s/keys :req-un [::timer-id]))

(s/def ::create-and-start-timer-args
  (s/keys :req-un [::timers-spec/project-id ::started-time ::created-time ::notes]))

(s/def ::update-timer-args
  (s/keys :req-un [::timer-id ::timers-spec/duration ::current-time ::notes]))


