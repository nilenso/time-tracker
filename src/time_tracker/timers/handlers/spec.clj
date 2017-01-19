(ns time-tracker.timers.handlers.spec
  (:require [time-tracker.spec :as core-spec]
            [clojure.spec :as s]))

(s/def ::date ::core-spec/epoch)
(s/def ::list-all-args
  (s/nilable (s/keys :req-un [::date])))
