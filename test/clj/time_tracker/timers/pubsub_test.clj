(ns time-tracker.timers.pubsub-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer [chan alt!! put!] :as async]
            [time-tracker.fixtures :as fixtures]
            [time-tracker.db :as db]
            [time-tracker.timers.db :as timers-db]
            [time-tracker.projects.test-helpers :as projects.helpers]
            [time-tracker.auth.test-helpers :as auth.test]
            [time-tracker.test-helpers :refer [populate-db] :as test-helpers]
            [time-tracker.util :as util]
            [gniazdo.core :as ws]
            [cheshire.core :as json]
            [clojure.spec :as sp]
            [time-tracker.timers.spec :as timers-spec]))

(use-fixtures :once fixtures/init! fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)

(deftest start-timer-command-test
  (let [task-ids1 (:task-ids (populate-db "gid1"))
        task-ids2 (:task-ids (populate-db "gid2"))
        current-time           (util/current-epoch-seconds)
        timer1                 (timers-db/create! (db/connection)
                                                  (first task-ids1)
                                                  "gid1"
                                                  current-time
                                                  "")
        timer2                 (timers-db/create! (db/connection)
                                                  (first task-ids2)
                                                  "gid2"
                                                  current-time
                                                  "")
        timer3                 (timers-db/create! (db/connection)
                                                  (first task-ids1)
                                                  "gid1"
                                                  current-time "")
        [response-chan socket] (test-helpers/make-ws-connection "gid1")]
    (try
      (testing "Owned timer"
        (ws/send-msg socket (json/encode
                             {:command      "start-timer"
                              :timer-id     (:id timer1)
                              :started-time current-time}))
        (let [command-response (test-helpers/try-take!! response-chan)]
          (is (= (:id timer1) (:id command-response)))))

      (testing "Unowned timer"
        (ws/send-msg socket (json/encode
                             {:command      "start-timer"
                              :timer-id     (:id timer2)
                              :started-time current-time}))
        (let [command-response (test-helpers/try-take!! response-chan)]
          (is (:error command-response))))

      (testing "Other started timers should be stopped"
        ;; At this point timer1 is started.
        (ws/send-msg socket (json/encode
                             {:command      "start-timer"
                              :timer-id     (:id timer3)
                              :started-time current-time}))
        (let [start-response (test-helpers/try-take!! response-chan)
              stop-response  (test-helpers/try-take!! response-chan)]
          (is (= (:id timer3) (:id start-response)))
          (is (= (:id timer1) (:id stop-response)))
          (is (not (nil? :started-time )))
          (is (nil? (:started-time stop-response)))))

      (finally (ws/close socket)))))

(deftest stop-timer-command-test
  (let [task-ids1 (:task-ids (populate-db "gid1"))
        task-ids2 (:task-ids (populate-db "gid2"))
        current-time           (util/current-epoch-seconds)
        timer1                 (timers-db/create! (db/connection)
                                                  (first task-ids1)
                                                  "gid1"
                                                  current-time
                                                  "")
        timer2                 (timers-db/create! (db/connection)
                                                  (first task-ids2)
                                                  "gid2"
                                                  current-time
                                                  "")
        [response-chan socket] (test-helpers/make-ws-connection "gid1")]
    (timers-db/start! (db/connection) (:id timer1) current-time)
    (timers-db/start! (db/connection) (:id timer2) current-time)
    (try
      (testing "Owned timer"
        (ws/send-msg socket (json/encode
                             {:command   "stop-timer"
                              :timer-id  (:id timer1)}))
        (let [command-response (test-helpers/try-take!! response-chan)]
          (is (sp/valid? ::timers-spec/duration (:duration command-response)))))

      (testing "Unowned timer"
        (ws/send-msg socket (json/encode
                             {:command   "stop-timer"
                              :timer-id  (:id timer2)}))
        (let [command-response (test-helpers/try-take!! response-chan)]
          (is (:error command-response))))
      (finally (ws/close socket)))))

(deftest delete-timer-command-test
  (let [task-ids1 (:task-ids (populate-db "gid1"))
        task-ids2 (:task-ids (populate-db "gid2"))
        current-time           (util/current-epoch-seconds)
        timer1                 (timers-db/create! (db/connection)
                                                  (first task-ids1)
                                                  "gid1"
                                                  current-time
                                                  "")
        timer2                 (timers-db/create! (db/connection)
                                                  (first task-ids2)
                                                  "gid2"
                                                  current-time
                                                  "")
        [response-chan socket] (test-helpers/make-ws-connection "gid1")]
    (try
      (testing "Owned timer"
        (testing "Timer exists"
          (ws/send-msg socket (json/encode
                               {:command   "delete-timer"
                                :timer-id  (:id timer1)}))
          (let [command-response (test-helpers/try-take!! response-chan)]
            (is (= "delete" (:type command-response)))
            (is (= (:id timer1)
                   (:id command-response)))))

        (testing "Timer does not exist"
          (ws/send-msg socket (json/encode
                               {:command  "delete-timer"
                                :timer-id (+ 5 (:id timer1))}))
          (let [command-response (test-helpers/try-take!! response-chan)]
            (is (:error command-response)))))

      (testing "Unowned timer"
        (ws/send-msg socket (json/encode
                             {:command   "delete-timer"
                              :timer-id  (:id timer2)}))
        (let [command-response (test-helpers/try-take!! response-chan)]
          (is (:error command-response))))

      (finally (ws/close socket)))))

(deftest create-timer-command-test
  (let [task-ids1 (:task-ids (populate-db "gid1"))
        task-ids2 (:task-ids (populate-db "gid2"))
        current-time           (util/current-epoch-seconds)
        [response-chan socket] (test-helpers/make-ws-connection "gid1")
        duration 60]
    (try
      (testing "Can track time on project"
        (ws/send-msg socket (json/encode
                             {:command      "create-timer"
                              :task-id   (first task-ids1)
                              :created-time current-time
                              :notes        "astro zombies"
                              :duration     duration}))
        (let [create-response (test-helpers/try-take!! response-chan)]
          (is (= (first task-ids1)
                 (:task-id create-response)))
          (is (= "create" (:type create-response)))
          (is (nil? (:started-time create-response)))
          (is (= "astro zombies"
                 (:notes create-response)))
          (is (= duration (:duration create-response)))
          (timers-db/delete! (db/connection) (:id create-response))))

      (println "Part of create-and-start-timer-command-test is currently disabled!")
      #_(testing "Can't track time on project"
          (ws/send-msg socket (json/encode
                               {:command      "create-and-start-timer"
                                :project-id   (get gen-projects "goo")
                                :started-time current-time
                                :created-time current-time
                                :notes        "robin hood in reverse"}))
          (let [command-response (test-helpers/try-take!! response-chan)]
            (is (:error command-response))))

      (finally (ws/close socket)))))

(deftest update-timer-command-test
  (let [task-ids1 (:task-ids (populate-db "gid1"))
        task-ids2 (:task-ids (populate-db "gid2"))
        current-time           (util/current-epoch-seconds)
        timer1                 (timers-db/create! (db/connection)
                                                  (first task-ids1)
                                                  "gid1"
                                                  current-time
                                                  "")
        timer2                 (timers-db/create! (db/connection)
                                                  (first task-ids2)
                                                  "gid2"
                                                  current-time
                                                  "wool heater")
        [response-chan socket] (test-helpers/make-ws-connection "gid1")]
    (try
      (testing "Owned timer"
        (timers-db/start! (db/connection) (:id timer1) current-time)
        (ws/send-msg socket (json/encode
                             {:command      "update-timer"
                              :timer-id     (:id timer1)
                              :duration     37
                              :notes        "brotherhood of the snake"}))
        (let [command-response (test-helpers/try-take!! response-chan)]
          (is (= (:id timer1) (:id command-response)))
          (is (= 37
                 (:duration command-response)))
          (is (= "brotherhood of the snake"
                 (:notes command-response)))
          (is (not (:error command-response)))))

      (testing "Unowned timer"
        (timers-db/start! (db/connection) (:id timer2) current-time)
        (ws/send-msg socket (json/encode
                             {:command      "update-timer"
                              :timer-id     (:id timer2)
                              :duration     37
                              :notes        "baz"}))
        (let [command-response (test-helpers/try-take!! response-chan)]
          (is (:error command-response))))

      (finally (ws/close socket)))))


(deftest ping-command-test
  (let [[response-chan socket] (test-helpers/make-ws-connection "gid1")]
    (try
      (testing "The server should respond to a ping with a pong"
        (ws/send-msg socket (json/encode
                             {:command "ping"}))
        (let [response (test-helpers/try-take!! response-chan)]
          (is (= "pong" (:type response)))))

      (finally (ws/close socket)))))


(deftest broadcast-test
  (let [task-ids1 (:task-ids (populate-db "gid1"))
        task-ids2 (:task-ids (populate-db "gid2"))
        task-id             (first task-ids1)
        current-time        (util/current-epoch-seconds)
        {timer-id :id}      (timers-db/create! (db/connection) task-id "gid1" current-time "")
        [response1 socket1] (test-helpers/make-ws-connection "gid1")
        [response2 socket2] (test-helpers/make-ws-connection "gid1")
        [response3 socket3] (test-helpers/make-ws-connection "gid2")]
    (try
      (ws/send-msg socket1 (json/encode
                            {:command      "start-timer"
                             :timer-id     timer-id
                             :started-time current-time}))
      (let [result1 (test-helpers/try-take!! response1)
            result2 (test-helpers/try-take!! response2)]
        (is (= timer-id (:id result1)))

        (testing "All clients with the same gid receive the broadcast"
          (is (= result1 result2))))

      (testing "Some other gid should not receive the broadcast"
        (let [result3  (alt!!
                         response3              ([value] value)
                         (async/timeout 100) ::not-received)]
          (is (= ::not-received
                 result3))))

      (finally
        (ws/close socket1)
        (ws/close socket2)
        (ws/close socket3)))))
