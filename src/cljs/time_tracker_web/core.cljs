(ns time-tracker-web.core
  (:require [reagent.dom]
            [re-frame.core :as rf]
            [time-tracker-web.events :as events]
            [time-tracker-web.views :as views]
            [time-tracker-web.google :as google]))

(defn render
  []
  (reagent.dom/render [views/root]
                      (js/document.getElementById "app")))

;; This is run by shadow-cljs after every reload
(defn ^:dev/after-load clear-cache-and-render!
  []
  (rf/clear-subscription-cache!)
  (render))

;; Entry point
(defn ^:export run
  []
  (google/init-api!)
  (rf/dispatch-sync [::events/initialize])
  (render))
