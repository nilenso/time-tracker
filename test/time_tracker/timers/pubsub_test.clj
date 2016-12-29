(ns time-tracker.timers.pubsub-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer [chan alt!! put!] :as async]
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

(use-fixtures :once fixtures/init! fixtures/migrate-test-db fixtures/serve-app)
(use-fixtures :each fixtures/isolate-db)

(def connect-url "ws://localhost:8000/api/timers/ws-connect/")

(defn- try-take!!
  [channel]
  (alt!!
    channel              ([value] value)
    (async/timeout 10000) (throw (ex-info "Take from channel timed out" {:channel channel}))))

(defn- try-deref
  [a-promise]
  (let [result (deref a-promise 1000 ::deref-timeout)]
    (if (= result ::deref-timeout)
      (throw (ex-info "Deref promise timed out" {:promise a-promise}))
      result)))

(defn make-ws-connection
  "Opens a connection and completes the auth handshake."
  [google-id]
  (let [response-chan (chan 5)
        socket        (ws/connect connect-url
                                  :on-receive #(put! response-chan
                                                     (json/decode % keyword)))]
    (ws/send-msg socket (json/encode
                         {:command "authenticate"
                          :token   (json/encode {:sub google-id})}))
    (if (= "success"
           (:auth-status (try-take!! response-chan)))
      [response-chan socket]
      (throw (ex-info "Authentication failed" {})))))


(deftest start-timer-command-test
  (let [gen-projects           (projects.helpers/populate-data! {"gid1" ["foo"]
                                                                 "gid2" ["goo"]})
        project-id             (get gen-projects "foo")
        timer1                 (timers.db/create! (db/connection) project-id "gid1")
        timer2                 (timers.db/create! (db/connection)
                                                  (get gen-projects "goo")
                                                  "gid2")
        timer3                 (timers.db/create! (db/connection)
                                                  (get gen-projects "foo")
                                                  "gid1")
        current-time           (util/current-epoch-seconds)
        [response-chan socket] (make-ws-connection "gid1")]
    (try
      (testing "Owned timer"
        (ws/send-msg socket (json/encode
                             {:command      "start-timer"
                              :timer-id     (:id timer1)
                              :started-time current-time}))
        (let [command-response (try-take!! response-chan)]
          (is (= (:id timer1) (:id command-response)))
          (is (= current-time (:started-time command-response)))))

      (testing "Unowned timer"
        (ws/send-msg socket (json/encode
                             {:command      "start-timer"
                              :timer-id     (:id timer2)
                              :started-time current-time}))
        (let [command-response (try-take!! response-chan)]
          (is (:error command-response))))

      (testing "Other started timers should be stopped"
        ;; At this point timer1 is started.
        (ws/send-msg socket (json/encode
                             {:command      "start-timer"
                              :timer-id     (:id timer3)
                              :started-time (+ 5 current-time)}))
        (let [start-response (try-take!! response-chan)
              stop-response  (try-take!! response-chan)]
          (is (= (:id timer3) (:id start-response)))
          (is (= (:id timer1) (:id stop-response)))
          (is (not (nil? :started-time )))
          (is (nil? (:started-time stop-response)))))
      
      (finally (ws/close socket)))))

(deftest stop-timer-command-test
  (let [gen-projects           (projects.helpers/populate-data! {"gid1" ["foo"]
                                                                 "gid2" ["goo"]})
        timer1                 (timers.db/create! (db/connection)
                                                  (get gen-projects "foo")
                                                  "gid1")
        timer2                 (timers.db/create! (db/connection)
                                                  (get gen-projects "goo")
                                                  "gid2")
        current-time           (util/current-epoch-seconds)
        stop-time              (+ current-time 7.0)
        [response-chan socket] (make-ws-connection "gid1")]
    (timers.db/start! (db/connection) (:id timer1) current-time)
    (timers.db/start! (db/connection) (:id timer2) current-time)
    (try
      (testing "Owned timer"
        (ws/send-msg socket (json/encode
                             {:command   "stop-timer"
                              :timer-id  (:id timer1)
                              :stop-time stop-time}))
        (let [command-response (try-take!! response-chan)]
          (is (s/valid? :timers.db/duration (:duration command-response)))
          (is (= 7
                 (:duration command-response)))))

      (testing "Unowned timer"
        (ws/send-msg socket (json/encode
                             {:command   "stop-timer"
                              :timer-id  (:id timer2)
                              :stop-time stop-time}))
        (let [command-response (try-take!! response-chan)]
          (is (:error command-response))))
      (finally (ws/close socket)))))

(deftest delete-timer-command-test
  (let [gen-projects           (projects.helpers/populate-data! {"gid1" ["foo"]
                                                                 "gid2" ["goo"]})
        timer1                 (timers.db/create! (db/connection)
                                                  (get gen-projects "foo")
                                                  "gid1")
        timer2                 (timers.db/create! (db/connection)
                                                  (get gen-projects "goo")
                                                  "gid2")
        [response-chan socket] (make-ws-connection "gid1")]
    (try
      (testing "Owned timer"
        (testing "Timer exists"
          (ws/send-msg socket (json/encode
                               {:command   "delete-timer"
                                :timer-id  (:id timer1)}))
          (let [command-response (try-take!! response-chan)]
            (is (= "delete" (:type command-response)))
            (is (= (:id timer1)
                   (:id command-response)))))

        (testing "Timer does not exist"
          (ws/send-msg socket (json/encode
                               {:command  "delete-timer"
                                :timer-id (+ 5 (:id timer1))}))
          (let [command-response (try-take!! response-chan)]
            (is (:error command-response)))))

      (testing "Unowned timer"
        (ws/send-msg socket (json/encode
                             {:command   "delete-timer"
                              :timer-id  (:id timer2)}))
        (let [command-response (try-take!! response-chan)]
          (is (:error command-response))))
      
      (finally (ws/close socket)))))

(deftest create-and-start-timer-command-test
  (let [gen-projects           (projects.helpers/populate-data! {"gid1" ["foo"]
                                                                 "gid2" ["goo"]})
        current-time           (util/current-epoch-seconds)
        [response-chan socket] (make-ws-connection "gid1")]
    (try
      (testing "Can track time on project"
        (ws/send-msg socket (json/encode
                             {:command      "create-and-start-timer"
                              :project-id   (get gen-projects "foo")
                              :started-time current-time}))
        (let [create-response (try-take!! response-chan)
              start-response  (try-take!! response-chan)]
          (is (= (get gen-projects "foo")
                 (:project-id create-response)))
          (is (= "create" (:type create-response)))
          (is (nil? (:started-time create-response)))
          (is (= current-time
                 (:started-time start-response)))))

      (println "Part of create-and-start-timer-command-test is currently disabled!")
      #_(testing "Can't track time on project"
        (ws/send-msg socket (json/encode
                             {:command      "create-and-start-timer"
                              :project-id   (get gen-projects "goo")
                              :started-time current-time}))
        (let [command-response (try-take!! response-chan)]
          (is (:error command-response))))
      
      (finally (ws/close socket)))))

(deftest change-timer-duration-command-test
  (let [gen-projects           (projects.helpers/populate-data! {"gid1" ["foo"]
                                                                 "gid2" ["goo"]})
        timer1                 (timers.db/create! (db/connection)
                                                  (get gen-projects "foo")
                                                  "gid1")
        timer2                 (timers.db/create! (db/connection)
                                                  (get gen-projects "goo")
                                                  "gid2")
        current-time           (util/current-epoch-seconds)
        [response-chan socket] (make-ws-connection "gid1")
        update-time            (+ current-time 17)]
    (try
      (testing "Owned timer"
        (timers.db/start! (db/connection) (:id timer1) current-time)
        (ws/send-msg socket (json/encode
                             {:command      "change-timer-duration"
                              :timer-id     (:id timer1)
                              :current-time update-time
                              :duration     37}))
        (let [command-response (try-take!! response-chan)]
          (is (= (:id timer1) (:id command-response)))
          (is (= update-time (:started-time command-response)))
          (is (= 37
                 (:duration command-response)))
          (is (not (:error command-response)))))

      (testing "Unowned timer"
        (timers.db/start! (db/connection) (:id timer2) current-time)
        (ws/send-msg socket (json/encode
                             {:command      "change-timer-duration"
                              :timer-id     (:id timer2)
                              :current-time update-time
                              :duration     37}))
        (let [command-response (try-take!! response-chan)]
          (is (:error command-response))))

      (finally (ws/close socket)))))


(deftest broadcast-test
  (let [gen-projects        (projects.helpers/populate-data! {"gid1" ["foo"]})
        project-id          (get gen-projects "foo")
        {timer-id :id}      (timers.db/create! (db/connection) project-id "gid1")
        current-time        (util/current-epoch-seconds)
        [response1 socket1] (make-ws-connection "gid1")
        [response2 socket2] (make-ws-connection "gid1")
        [response3 socket3] (make-ws-connection "gid2")]
    (try
      (ws/send-msg socket1 (json/encode
                            {:command      "start-timer"
                             :timer-id     timer-id
                             :started-time current-time}))
      (let [result1 (try-take!! response1)
            result2 (try-take!! response2)]
        (is (= timer-id (:id result1)))
        (is (= current-time (:started-time result2)))

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
