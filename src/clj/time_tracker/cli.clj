(ns time-tracker.cli
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]))

(defonce ^:private opts-atom (atom nil))

;; Our cli takes some options and one optional argument which denotes the mode of operation
;; The operation defaults to "serve"

(def cli-options
  [["-f" "--config-file FILE" "Path to configuration file"]
   ["-r" "--rollback" "Rollback the last migration. Must be run alone"]
   ["-m" "--migrate" "Run migrations"]
   ["-h" "--help" "Print this help message"]
   ["-s" "--serve" "Run the web server"]])

(defn init! [args]
  (let [{:keys [arguments options errors] :as parsed-opts} (parse-opts args cli-options)]
    (when-not errors
      (reset! opts-atom parsed-opts))))

(defn opts []
  (:options @opts-atom))

(defn help-message []
  (str/join "\n\n" ["The time-tracker server"
                    "Use only one option of -s -m -r or -h at once"
                    (:summary @opts-atom)]))
