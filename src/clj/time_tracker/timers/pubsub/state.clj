(ns time-tracker.timers.pubsub.state)

;; A map of channels to google IDs
(defonce channel->google-id (atom {}))

(defn- conj-to-set
  "If the-set is nil, returns a new set containing value.
  Behaves like conj otherwise."
  [the-set value]
  (if (nil? the-set)
    #{value}
    (conj the-set value)))

(defn add-channel!
  "Adds a channel to the connection state."
  [channel google-id]
  (swap! channel->google-id assoc channel google-id))

(defn remove-channel!
  "Removes a channel from the connection state."
  [channel]
  (if-let [google-id (get @channel->google-id channel)]
    (swap! channel->google-id dissoc channel)))

(defn active-connections [google-id]
  (->> @channel->google-id
       (filter (fn [[ch gid]] (= gid google-id)))
       keys))
