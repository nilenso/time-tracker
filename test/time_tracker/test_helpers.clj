(ns time-tracker.test-helpers
  (:require [time-tracker.auth.test-helpers :as auth.helpers]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.core.async :refer [chan alt!! put!] :as async]
            [gniazdo.core :as ws]
            [clojure.test :as test]
            [clojure.spec.test :as stest]
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
  (async/<!! channel))

(defn make-ws-connection
  "Opens a connection and completes the auth handshake."
  [google-id]

  (let [response-chan (chan 5)
        result (promise)
        conn (ws/connect "ws://localhost:8000/api/timers/ws-connect/"
                                  :on-receive #(put! response-chan (json/decode % keyword))
                                  :on-error (fn on-error [ex] (deliver result ex)))]
    (ws/send-msg conn (json/encode
                         {:command "authenticate"
                          :token   (json/encode {:sub google-id})}))

    (if (deref result 5 nil)
      (throw (ex-info "WS Connection could not be created!" {:error @result}))) 

    (if (= "success"
           (:auth-status (try-take!! response-chan)))
      [response-chan conn]
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
