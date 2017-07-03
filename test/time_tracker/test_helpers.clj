(ns time-tracker.test-helpers
  (:require [time-tracker.auth.test-helpers :as auth.helpers]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.core.async :refer [chan alt!! put!] :as async]
            [gniazdo.core :as ws]
            [clojure.test :as test]
            [clojure.spec.test :as stest]
            [time-tracker.logging :as log]
            [time-tracker.util :as util]))

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
        socket        (promise)]

    (deliver socket (ws/connect "ws://localhost:8000/api/timers/ws-connect/"
                                :on-receive (fn [data]
                                              (log/debug {:event ::received-ws-data
                                                          :data  data})
                                              (put! response-chan (json/decode data keyword)))
                                :on-close (fn on-close [status desc]
                                            (log/debug {:event       ::closed-ws-connection
                                                        :status      status
                                                        :description desc})) 
                                :on-error (fn [ex]
                                            (log/error ex {:event ::ws-error}))
                                :on-connect (fn [_]
                                              (log/debug {:event ::established-ws-connection})
                                              (ws/send-msg @socket
                                                           (json/encode
                                                            {:command "authenticate"
                                                             :token   (json/encode {:sub google-id})}))
                                              (log/debug {:event ::sent-authentication-message}))))
    
    (if (= "success"
           (:auth-status (try-take!! response-chan)))
      [response-chan @socket]
      (throw (ex-info "Authentication failed" {})))))

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
