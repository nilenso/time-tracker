(ns time-tracker.timers.pubsub.routes
  (:require [time-tracker.timers.pubsub.middleware :as middleware]
            [time-tracker.timers.pubsub.commands :as commands]
            [clojure.algo.generic.functor :refer [fmap]]
            [time-tracker.timers.pubsub.spec :as pubsub-spec]))

(defn- wrap-middlewares
  "Wraps commands in a command map with the give set of middleware.
  The first middleware is the 'outermost'."
  [middlewares command-map]
  (fmap (apply comp middlewares) command-map))

;; For now, anyone can log time against any project.
(def command-map
  (wrap-middlewares
   [middleware/wrap-exception middleware/wrap-transaction]
   (merge {"create-and-start-timer" (-> commands/create-and-start-timer!
                                        ;;(middleware/wrap-can-create-timer)
                                        (middleware/wrap-validator
                                         ::pubsub-spec/create-and-start-timer-args))}

          {"start-timer"            (-> commands/start-timer!
                                        (middleware/wrap-owns-timer)
                                        (middleware/wrap-validator ::pubsub-spec/start-timer-args))
           "stop-timer"             (-> commands/stop-timer!
                                        (middleware/wrap-owns-timer)
                                        (middleware/wrap-validator ::pubsub-spec/stop-timer-args))
           "delete-timer"           (-> commands/delete-timer!
                                        (middleware/wrap-owns-timer)
                                        (middleware/wrap-validator ::pubsub-spec/delete-timer-args))
           "update-timer"           (-> commands/update-timer!
                                        (middleware/wrap-owns-timer)
                                        (middleware/wrap-validator
                                         ::pubsub-spec/update-timer-args))
           "ping"                   commands/receive-ping!})))
