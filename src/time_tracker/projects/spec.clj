(ns time-tracker.projects.spec
  (:require [clojure.spec :as s]
            [clojure.string :as str]
            [time-tracker.spec :as core-spec]
            [time-tracker.clients.spec :as clients-spec]))

(s/def ::id ::core-spec/id)
(s/def ::name ::core-spec/non-empty-string)
(s/def ::client-id ::clients-spec/id)

(s/def ::project
  (s/keys :req-un [::id ::name]))

(s/def ::project-create-input
  (s/keys :req-un [::name ::client-id]))

(s/def ::project-modify-input
  (s/keys :req-un [::name]))
