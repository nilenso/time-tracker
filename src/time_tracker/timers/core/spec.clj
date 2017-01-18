(ns time-tracker.timers.core.spec
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [time-tracker.timers.spec :as timers-spec]
            [time-tracker.timers.core :as timers-core]))

(defn- clamp-pred
  [{:keys [args ret]}]
  (or (nil? ret)
      (timers-core/inside? ret
                           (:start-epoch args)
                           (:end-epoch args))))

(s/fdef timers-core/clamp
        :args (s/and (s/cat :timer ::timers-spec/timer
                            :start-epoch ::timers-spec/epoch
                            :end-epoch ::timers-spec/epoch)
                     (fn [{:keys [start-epoch end-epoch]}]
                       (< start-epoch end-epoch)))
        :ret (s/nilable ::timers-spec/timer)
        :fn clamp-pred)

