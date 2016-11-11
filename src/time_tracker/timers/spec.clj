(ns time-tracker.timers.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec]))

(s/def ::id number?)
(s/def ::epoch :core/positive-num)

(s/def ::hours :core/positive-num)
(s/def ::minutes :core/positive-num)
(s/def ::seconds :core/positive-num)

(s/def ::timer-id ::id)
(s/def ::started-time ::epoch)

(s/def :timers.pubsub/start-timer-args
  (s/keys :req-un [::timer-id ::started-time]))
