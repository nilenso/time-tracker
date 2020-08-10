(ns time-tracker.tasks.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec :as core-spec]
            [time-tracker.projects.spec :as projects-spec]))

(s/def ::id ::core-spec/id)
(s/def ::name ::core-spec/non-empty-string)
(s/def ::project-id ::projects-spec/id)

(s/def ::task-create-input
  (s/keys ::req-un [::name ::project-id]))
