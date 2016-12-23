(ns time-tracker.timers.pubsub.state)

;; Connection management -------

;; A map of google-ids to sets of channels.
(defonce active-connections (atom {}))

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
  (swap! active-connections update google-id conj-to-set channel)
  (swap! channel->google-id assoc channel google-id))

(defn remove-channel!
  "Removes a channel from the connection state."
  [channel]
  (if-let [google-id (get @channel->google-id channel)]
    (do
      (swap! active-connections update google-id disj channel)
      (swap! channel->google-id dissoc channel))))
