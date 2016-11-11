(ns time-tracker.timers.pubsub-test
  (:require [clojure.test :refer :all]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers.db]
            [time-tracker.projects.test-helpers :as projects.helpers]
            [time-tracker.auth.test-helpers :as auth.test]
            [time-tracker.util :as util]
            [gniazdo.core :as ws]
            [cheshire.core :as json]))

(use-fixtures :once fixtures/init-db! fixtures/migrate-test-db fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)

(def connect-url "ws://localhost:8000/timers/ws-connect/")

(deftest start-timer-command
  (let [gen-projects     (projects.helpers/populate-data! {"gid1" ["foo"]})
        project-id       (get gen-projects "foo")
        {timer-id :id}   (timers.db/create-if-authorized! project-id "gid1")
        current-time     (util/current-epoch-seconds)
        command-response (promise)
        socket           (ws/connect connect-url
                                     :headers    (auth.test/fake-login-headers "gid1")
                                     :on-connect (fn [_] (println "Successfully connected"))
                                     :on-receive #(deliver command-response (json/decode % keyword)))]
    (try
      (ws/send-msg socket (json/encode
                           {:command      "start-timer"
                            :timer-id     timer-id
                            :started-time current-time}))
      (is (= timer-id (get @command-response :timer-id)))
      (is (= current-time (get @command-response :started-time)))
      (finally (ws/close socket)))))
