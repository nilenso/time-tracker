(ns time-tracker.data-import.harvest.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec :as core-spec]))

(s/def ::id ::core-spec/id)
(s/def ::name string?)

(s/def ::client
  (s/keys :req-un [::id ::name]))

(s/def ::client-id ::id)
(s/def ::project
  (s/keys :req-un [::id ::client-id ::name]))

(s/def ::task
  (s/keys :req-un [::id ::name]))

(s/def ::task-id ::id)
(s/def ::project-id ::id)
(s/def ::task-assignment
  (s/keys :req-un [::task-id ::project-id]))
