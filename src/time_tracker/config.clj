(ns time-tracker.config
  (:require [clojure.java.io :as io]
            [nomad :refer [defconfig]]))

(defconfig app-config (io/file "config/backend-config.edn"))

(def google-tokeninfo-url (:google-tokeninfo-url (app-config)))
