(ns time-tracker.timers.handlers.spec
  (:require [time-tracker.timers.spec :as timers-spec]
            [clojure.spec :as s]))

(s/def ::date ::timers-spec/epoch)
(s/def ::list-all-args
  (s/nilable (s/keys :req-un [::date])))
