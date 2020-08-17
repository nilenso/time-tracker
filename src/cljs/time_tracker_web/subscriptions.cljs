(ns time-tracker-web.subscriptions
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::google-client-initialized?
  (fn [db _]
    (:google-client-initialized? db)))

(rf/reg-sub
  ::signed-in?
  (fn [db _]
    (:signed-in? db)))
