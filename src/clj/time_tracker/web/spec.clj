(ns time-tracker.web.spec
  (:require [time-tracker.spec :as core-spec]
            [clojure.spec :as s]))

(s/def ::start ::core-spec/epoch)
(s/def ::end ::core-spec/epoch)

(s/def ::start-end-epoch-params
  (s/and (s/keys :req-un [::start ::end])
         (fn [{:keys [start end]}]
           (< start end))))
