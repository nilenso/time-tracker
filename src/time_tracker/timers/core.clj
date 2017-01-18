(ns time-tracker.timers.core
  (:require [clj-time.coerce]
            [clj-time.core :as clj-time]
            [time-tracker.util :as util]))

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

(defn elapsed-time
  "Number of seconds the timer has tracked,"
  [{:keys [started-time duration]}]
  (if started-time
    (+ duration (- (util/current-epoch-seconds) started-time))
    duration))

(defn outside?
  "Is the timer completely outside the time period?"
  [{:keys [time-created duration] :as timer} start-epoch end-epoch]
  (or (< (+ time-created duration) start-epoch)
      (>= time-created end-epoch)))

(defn ended-within?
  "Has the timer ended within the time period?"
  [{:keys [time-created duration] :as timer} start-epoch end-epoch]
  (and (not (outside? timer start-epoch end-epoch))
       (< (+ time-created duration) end-epoch)))

(defn started-within?
  "Has the timer started within the time period?"
  [{:keys [time-created] :as timer} start-epoch end-epoch]
  (and (not (outside? timer start-epoch end-epoch))
       (<= start-epoch time-created)))

(defn inside?
  "Is the timer completely inside the time period?"
  [timer start-epoch end-epoch]
  (and (started-within? timer start-epoch end-epoch)
       (ended-within? timer start-epoch end-epoch)))

(defn clamp
  "Returns the part of the timer that is between `start-epoch` and `end-epoch`.
  `end-epoch` is exclusive.
  If the timer is in between already, returns it unchanged.
  If the timer runs into/out of the time period, then its `time-created` or 
  `duration` are updated accordingly.
  Returns `nil` if the timer is completely outside the duration or if the timer 
  is still running."
  [{:keys [time-created duration started-time] :as timer} start-epoch end-epoch]
  (cond
    (some? started-time)                          nil
    (outside? timer start-epoch end-epoch)        nil
    (inside? timer start-epoch end-epoch)         timer
    (ended-within? timer start-epoch end-epoch)   (merge timer {:time-created start-epoch
                                                                :duration     (- duration (- start-epoch time-created))})
    (started-within? timer start-epoch end-epoch) (assoc timer :duration (- end-epoch time-created 1))
    :else                                         nil))
