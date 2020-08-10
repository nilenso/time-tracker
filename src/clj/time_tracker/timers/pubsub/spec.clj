(ns time-tracker.timers.pubsub.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec :as core-spec]
            [time-tracker.timers.spec :as timers-spec]))

(s/def ::timer-id ::timers-spec/id)
(s/def ::created-time ::timers-spec/time-created)
(s/def ::stop-time ::core-spec/epoch)
(s/def ::notes ::timers-spec/notes)

(s/def ::start-timer-now-args
  (s/keys :req-un [::timer-id]))

(s/def ::stop-timer-now-args
  (s/keys :req-un [::timer-id]))

(s/def ::delete-timer-args
  (s/keys :req-un [::timer-id]))

(s/def ::create-timer-args
  (s/keys :req-un [::timers-spec/task-id ::created-time ::notes]))

(s/def ::update-timer-now-args
  (s/keys :req-un [::timer-id ::timers-spec/duration ::notes]))
