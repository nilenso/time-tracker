(ns time-tracker.timers.pubsub-test
  (:require [clojure.test :refer :all]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers.db]
            [time-tracker.projects.test-helpers :as projects.helpers]
            [time-tracker.auth.test-helpers :as auth.test]
            [time-tracker.util :as util]
            [gniazdo.core :as ws]
            [cheshire.core :as json]
            [clojure.spec :as s]
            [time-tracker.timers.spec]))

(use-fixtures :once fixtures/init-db! fixtures/migrate-test-db fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)

(def connect-url "ws://localhost:8000/timers/ws-connect/")

(deftest start-timer-command
  (let [gen-projects     (projects.helpers/populate-data! {"gid1" ["foo"]})
        project-id       (get gen-projects "foo")
        {timer-id :id}   (timers.db/create-if-authorized! (db/connection) project-id "gid1")
        current-time     (util/current-epoch-seconds)
        command-response (promise)
        socket           (ws/connect connect-url
                                     :headers    (auth.test/fake-login-headers "gid1")
                                     :on-receive #(deliver command-response (json/decode % keyword)))]
    (try
      (ws/send-msg socket (json/encode
                           {:command      "start-timer"
                            :timer-id     timer-id
                            :started-time current-time}))
      (is (= timer-id (get @command-response :timer-id)))
      (is (= current-time (get @command-response :started-time)))
      (finally (ws/close socket)))))

(deftest stop-timer-command
  (let [gen-projects     (projects.helpers/populate-data! {"gid1" ["foo"]})
        project-id       (get gen-projects "foo")
        {timer-id :id}   (timers.db/create-if-authorized! (db/connection) project-id "gid1")
        current-time     (util/current-epoch-seconds)
        stop-time        (+ current-time 7.0)
        command-response (promise)
        socket           (ws/connect connect-url
                                     :headers    (auth.test/fake-login-headers "gid1")
                                     :on-receive #(deliver command-response (json/decode % keyword)))]
    (timers.db/start-if-authorized! (db/connection) timer-id current-time "gid1")
    (try
      (ws/send-msg socket (json/encode
                           {:command   "stop-timer"
                            :timer-id  timer-id
                            :stop-time stop-time}))
      (is (s/valid? :timers.db/duration (:duration @command-response)))
      (is (= 7.0
             (get-in @command-response [:duration :seconds])))
      (finally (ws/close socket)))))

(deftest delete-timer-command
  (let [gen-projects     (projects.helpers/populate-data! {"gid1" ["foo"]})
        project-id       (get gen-projects "foo")
        {timer-id :id}   (timers.db/create-if-authorized! (db/connection) project-id "gid1")
        command-response (promise)
        socket           (ws/connect connect-url
                                     :headers    (auth.test/fake-login-headers "gid1")
                                     :on-receive #(deliver command-response (json/decode % keyword)))]
    (try
      (testing "Timer exists"
        (ws/send-msg socket (json/encode
                             {:command   "delete-timer"
                              :timer-id  timer-id}))
        (is (:delete? @command-response))
        (is (= timer-id
               (:timer-id @command-response))))

      #_(testing "Timer does not exist"
        (ws/send-msg socket (json/encode
                             {:command  "delete-timer"
                              :timer-id (+ 5 timer-id)}))
        (is (:error @command-response))
        (println  @command-response))
      (finally (ws/close socket)))))
