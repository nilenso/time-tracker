(ns time-tracker.timers.core
  (:require [clj-time.coerce]
            [clj-time.core :as clj-time]))

(defn- epoch->clj-time
  [epoch utc-offset]
  (-> (clj-time.coerce/from-long (* 1000 (long epoch)))
      (clj-time/to-time-zone (clj-time/time-zone-for-offset (int (/ utc-offset 60))
                                                            (rem utc-offset 60)))))

(defn same-day?
  "True if `epoch2` is on the same day as `epoch1`.
  Expects a `utc-offset` in minutes.
  Assumes epochs are just numbers."
  [epoch1 epoch2 utc-offset]
  (let [timezone     (clj-time/time-zone-for-offset (int (/ utc-offset 60))
                                                    (rem utc-offset 60))
        time1        (epoch->clj-time epoch1 utc-offset)
        time2        (epoch->clj-time epoch2 utc-offset)
        lower-bound  (-> (clj-time/date-time (clj-time/year  time1)
                                             (clj-time/month time1)
                                             (clj-time/day   time1))
                         (clj-time/from-time-zone timezone))
        upper-bound  (clj-time/plus lower-bound
                                    (clj-time/days 1))
        day-interval (clj-time/interval lower-bound upper-bound)]
    (clj-time/within? day-interval time2)))

(defn created-on?
  "True if a timer was created on the same day as the given epoch.
  Assumes that the epoch is a number."
  [timer epoch utc-offset]
  (same-day? epoch (:time-created timer) utc-offset))
