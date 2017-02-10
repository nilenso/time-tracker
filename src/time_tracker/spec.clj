(ns time-tracker.spec
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [time-tracker.util :as util]))

;; General useful specs

(def minutes-in-hour 60)

(s/def ::positive-num (s/and number?
                             #(>= % 0)
                             #(java.lang.Double/isFinite %)))
(s/def ::positive-int (s/and int? #(>= % 0)))
(s/def ::id ::positive-int)

(defn- epoch-gen []
  (->> (s/gen ::positive-int)
       (gen/fmap #(long (/ % 1000)))))

(s/def ::epoch (s/with-gen ::positive-int
                           epoch-gen))

(s/def ::start ::epoch)
(s/def ::end ::epoch)
(s/def ::epoch-range
  (s/and (s/keys :req-un [::start ::end])
         (fn [{:keys [start end]}]
           (< start end))))

(s/def ::utc-offset (s/int-in (- (* 12 minutes-in-hour))
                              (+ 1 (* 14 minutes-in-hour))))
(s/def ::non-empty-string (s/and string? seq))

(s/def ::positive-bigdec (s/and bigdec?
                                #(>= % 0)))

(defn- positive-bigdec-gen [num-places]
  (->> (s/gen ::positive-num)
       (gen/fmap #(util/bigdec-places % num-places))))

(defn- positive-bigdec-range-gen [num-places lower-bound upper-bound]
  (->> (s/gen (s/double-in :min lower-bound :max upper-bound :NaN? false :infinite? false))
       (gen/fmap #(util/bigdec-places % num-places))
       (gen/such-that #(<= lower-bound % upper-bound))))

(defn positive-bigdec
  ([num-places]
   (s/with-gen (s/and ::positive-bigdec
                      #(= num-places (.scale %)))
               (partial positive-bigdec-gen num-places)))
  ([num-places lower-bound upper-bound]
   {:pre [(<= 0M lower-bound)
          (< lower-bound upper-bound)]}
    (s/with-gen (s/and ::positive-bigdec
                       #(= num-places (.scale %)))
                (partial positive-bigdec-range-gen num-places lower-bound upper-bound))))

(s/def ::money-val (positive-bigdec 2))
