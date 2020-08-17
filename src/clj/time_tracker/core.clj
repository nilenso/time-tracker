(ns time-tracker.core
  (:gen-class)
  (:require [mount.core :as mount]
            [mount-up.core :as mu]
            [time-tracker.migration :refer [migrate-db rollback-db]]
            [time-tracker.cli :as cli]
            [time-tracker.web.service :as web-service]
            [taoensso.timbre :as log]))

(defn- log-mount-action [action-map]
  (fn [{:keys [name action]}]
    (log/info {:event (action-map action)
               :state name})))

(defn mount-init! [opts]
  (mu/all-clear)
  (mu/on-upndown :before-info
                 (log-mount-action {:up ::state-up-pre
                                    :down ::state-down-pre})
                 :before)
  (mu/on-upndown :after-info
                 (log-mount-action {:up ::state-up-post
                                    :down ::state-down-post})
                 :after)
  (mu/on-up :around-exceptions
            (mu/try-catch
             (fn [ex {:keys [name]}] (log/error ex {:event ::state-up-failure
                                                    :state name
                                                    :exception ex})))
            :wrap-in)
  (mount/start-with-args opts))

(defn -main
  [& args]
  (let [opts (cli/parse args)]
    (if-let [opts-error (cli/error-message opts)]
      (do
        (print opts-error)
        (System/exit 1))
      (case (cli/operational-mode opts)
        :migrate (do (mount-init! opts)
                     (migrate-db))
        :rollback (do (mount-init! opts)
                      (rollback-db))
        :help (print (cli/help-message opts))
        :serve (do (mount-init! opts)
                   (web-service/start-server!))
        (print (cli/help-message opts))))))
