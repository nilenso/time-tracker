(ns time-tracker.web.util
  (:require [clojure.spec :as s]))

(defn validate-request
  [request spec]
  (when-not (s/valid? spec (:body request))
    (throw (ex-info "Validation failed" {:event   :validation-failed
                                         :spec    spec
                                         :request request}))))
