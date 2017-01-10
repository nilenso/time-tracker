(ns time-tracker.projects.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec :as core-spec]))

(s/def ::id ::core-spec/id)
(s/def ::name string?)

(s/def ::project
  (s/keys :req-un [::id ::name]))
