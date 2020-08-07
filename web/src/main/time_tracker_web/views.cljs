(ns time-tracker-web.views
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [time-tracker-web.subscriptions :as subs]
            [time-tracker-web.events :as events]
            ["react-day-picker" :as DayPicker]))

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
  (let [selected-day @(rf/subscribe [::subs/selected-day])]
    [:div
     [:h1 "Welcome to TT, it is now"]
     [clock]
     [color-input]
     [:h2 "Here's a day picker for you"]
     [:> DayPicker {:on-day-click  (fn [day options]
                                     (let [{:keys [disabled]} (js->clj options :keywordize-keys true)]
                                       (when-not disabled
                                         (rf/dispatch [::events/selected-day-changed day]))))
                    :selected-day  selected-day
                    :disabled-days (clj->js {:daysOfWeek [0]})}]
     [:h2 (str "The selected day is " (some-> selected-day
                                              .toLocaleDateString))]]))
