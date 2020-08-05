(ns time-tracker-web.core
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [time-tracker-web.events :as events]
            [time-tracker-web.views :as views]))

(defn dispatch-timer-event
  []
  (let [now (js/Date.)]
    (rf/dispatch [::events/timer now])))

(defonce do-timer (js/setInterval dispatch-timer-event 1000))

(defn render
  []
  (reagent.dom/render [views/ui]
                      (js/document.getElementById "app")))

;; This is run by shadow-cljs after every reload
(defn ^:dev/after-load clear-cache-and-render!
  []
  (rf/clear-subscription-cache!)
  (render))

;; Entry point
(defn ^:export run
  []
  (rf/dispatch-sync [::events/initialize])
  (render))
