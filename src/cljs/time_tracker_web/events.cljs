(ns time-tracker-web.events
  (:require [re-frame.core :as rf]
            [time-tracker-web.db :as db]))

(rf/reg-event-db
  ::initialize
  (fn [_ _]
    db/default-db))

(rf/reg-event-db
  ::google-client-initialized
  (fn [db _]
    (assoc db :google-client-initialized? true)))

(rf/reg-event-db
  ::update-id-token
  (fn [db [_ id-token]]
    (assoc db
           :google-id-token id-token
           :signed-in? true)))
