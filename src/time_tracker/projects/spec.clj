(ns time-tracker.projects.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec]))

(s/def ::id :core/id)
(s/def ::name string?)

(s/def :projects.db/project
  (s/keys :req-un [::id ::name]))
