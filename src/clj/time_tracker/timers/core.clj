(ns time-tracker.timers.core
  (:require [clj-time.coerce]
            [clj-time.core :as clj-time]
            [time-tracker.util :as util]))

(defn elapsed-time
  "Number of seconds the timer has tracked,"
  [{:keys [started-time duration]}]
  (if started-time
    (+ duration (- (util/current-epoch-seconds) started-time))
    duration))
