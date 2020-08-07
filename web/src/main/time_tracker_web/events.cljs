(ns time-tracker-web.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
  ::initialize
  (fn [_ _]
    {:time       (js/Date.)
     :time-color "#f88"
     :selected-day nil}))

(rf/reg-event-db
  ::time-color-change
  (fn [db [_ new-color-value]]
    (assoc db :time-color new-color-value)))

(rf/reg-event-db
  ::timer
  (fn [db [_ new-time]]
    (assoc db :time new-time)))

(rf/reg-event-db
  ::selected-day-changed
  (fn [db [_ new-day]]
    (assoc db :selected-day new-day)))

