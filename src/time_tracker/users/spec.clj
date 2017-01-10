(ns time-tracker.users.spec
  (:require [clojure.spec :as s]
            [time-tracker.spec]))

(s/def ::id :core/id)
(s/def ::google-id string?)
(s/def ::name string?)
(s/def ::role string?)

(s/def ::user
  (s/keys :req-un [::id ::google-id ::name ::role]))
