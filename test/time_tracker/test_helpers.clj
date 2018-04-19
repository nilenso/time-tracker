(ns time-tracker.test-helpers
  (:require [time-tracker.auth.test-helpers :as auth.helpers]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.core.async :refer [chan alt!! put!] :as async]
            [gniazdo.core :as ws]
            [clojure.test :as test]
            [clojure.spec.test :as stest]
            [time-tracker.logging :as log]
            [time-tracker.util :as util]
            [clojure.string :as str]
            [time-tracker.db :as db]
            [time-tracker.clients.test-helpers :as clients.helpers]
            [time-tracker.projects.test-helpers :as projects.helpers]
            [time-tracker.tasks.test-helpers :as tasks.helpers]))

(defn settings
  "Reads a profile and returns a specific setting"
  [setting]
  (let [port (Integer/parseInt (util/from-config :port))
        host (str/join ["http://localhost:" port "/"])
        host-ws (str/join ["ws://localhost:" port "/"])
        ws-url (str/join [host-ws "api/timers/ws-connect/"])
        api-root (str/join [host "api/"])]
    (setting {:port port :host host :host-ws host-ws :ws-url ws-url :api-root api-root})))

(defn http-request-raw
  "Makes a HTTP request. Does not process the body."
  ([method url google-id] (http-request-raw method url google-id nil))
  ([method url google-id body] (http-request-raw method url google-id body "Agent Smith"))
  ([method url google-id body name]
   (let [params   (merge {:url url
                          :method method
                          :headers (merge (auth.helpers/fake-login-headers google-id name)
                                          {"Content-Type" "application/json"})
                          :as :text}
                         (if body {:body (json/encode body)}))
         response @(http/request params)]
     response)))

(defn http-request
  "Makes a HTTP request. Decodes the JSON body."
  [& args]
  (let [response (apply http-request-raw args)]
    (assoc response :body (json/decode (:body response)))))

(defn try-take!!
  [channel]
  (alt!!
    channel ([value] value)
    (async/timeout 10000) (throw (ex-info "Take from channel timed out" {:channel channel}))))

(defn make-ws-connection
  "Opens a connection and completes the auth handshake."
  [google-id]
  (let [response-chan (chan 5)
        socket        (ws/connect (settings :ws-url)
                                :on-receive (fn on-receive
                                              [data]
                                              (log/debug {:event ::received-ws-data
                                                          :data  data})
                                              (put! response-chan (json/decode data keyword)))
                                :on-close (fn on-close
                                            [status desc]
                                            (log/debug {:event       ::closed-ws-connection
                                                        :status      status
                                                        :description desc})) 
                                :on-error (fn on-error
                                            [ex]
                                            (log/error ex {:event ::ws-error}))
                                :on-connect (fn on-connect
                                              [_]
                                              (log/debug {:event ::established-ws-connection})))]

    ;; Waiting for an initial "ready" message from the server so that
    ;; we can send subsequent messages to it without worrying about them
    ;; getting dropped.
    ;; https://github.com/http-kit/http-kit/issues/318
    (if (= "ready" (:type (try-take!! response-chan)))
      (do
        (ws/send-msg socket (json/encode
                         {:command "authenticate"
                          :token   (json/encode {:sub google-id})}))
        (if (= "success"
               (:auth-status (try-take!! response-chan)))
          [response-chan socket]
          (throw (ex-info "Authentication failed" {}))))
      (throw (ex-info "Server not ready" {})))
    ))

(defn- num-tests-from-config []
  (Integer/parseInt (util/from-config :num-tests)))

(defn assert-generative-test
  ([sym] (assert-generative-test sym {:num-tests (num-tests-from-config)}))
  ([sym opts]
   (test/is (empty? (->> (stest/check sym
                                      {:clojure.spec.test.check/opts
                                       (merge {:num-tests (num-tests-from-config)} opts)})
                         (map stest/abbrev-result)
                         (filter :failure)
                         (map :failure))))))

(defn populate-db
  [google-id]
  (let [client-name "FooClient"
        client-id (:id (clients.helpers/create-client! (db/connection) {:name client-name}))
        project-name->project-ids (projects.helpers/populate-data! {google-id ["pr1" "pr2"]}
                                                                   client-id)
        task-ids (map (fn [task-name project-id]
                        (tasks.helpers/create-task! (db/connection)
                                                    task-name
                                                    project-id))
                      ["task1" "task2"]
                      (vals project-name->project-ids))]
    {:task-ids task-ids
     :project-name->project-ids project-name->project-ids}))
