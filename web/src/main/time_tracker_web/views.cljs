(ns time-tracker-web.views
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [time-tracker-web.subscriptions :as subs]
            [time-tracker-web.events :as events]))

(defn clock
  []
  [:div.example-clock
   {:style {:color @(rf/subscribe [::subs/time-color])}}
   (-> @(rf/subscribe [::subs/time])
       .toTimeString
       (str/split " ")
       first)])

(defn color-input
  []
  [:div.color-input
   "Time color: "
   [:input {:type      "text"
            :value     @(rf/subscribe [::subs/time-color])
            :on-change #(rf/dispatch [::events/time-color-change (-> % .-target .-value)])}]])

(defn ui
  []
  [:div
   [:h1 "Welcome to TT, it is now"]
   [clock]
   [color-input]])
