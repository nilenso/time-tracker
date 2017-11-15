(ns time-tracker.clients.spec
  (:require [clojure.spec :as s]
            [clojure.string :as str]
            [time-tracker.spec :as core-spec]))

(s/def ::name ::core-spec/non-empty-string)
(s/def ::address string?)
(s/def ::gstin (s/and string? #(= 15 (count %))))
(s/def ::pan (s/and string? #(= 10 (count %))))

(s/def ::phone (s/and string? #(<= (count %) 20)))
(s/def ::email (s/and string? #(<= (count %) 100)))
(s/def ::point-of-contact (s/keys :req-un [::name]
                                  :opt-un [::phone ::email]))
(s/def ::points-of-contact (s/coll-of ::point-of-contact))

(s/def ::client (s/keys :req-un [::name ::address ::gstin ::pan]
                        :opt-un [::points-of-contact]))
