(ns time-tracker.auth.core-test
  (:require [clojure.test :refer :all]
            [time-tracker.auth.core :as auth]
            [ring.util.response :as res]
            [time-tracker.util :refer [map-contains?]]
            [time-tracker.config :as config]))

(deftest token-from-headers-test
  (testing "Header absent"
    (is (nil? (auth/token-from-headers {"content-type" "application/json"}))))

  (testing "Header with different scheme"
    (is (nil? (auth/token-from-headers {"authorization" "Digest a3451cbd4520e0ff02d"}))))

  (testing "Correct header"
    (is (= "token"
           (auth/token-from-headers {"authorization" "Bearer token"})))))

(deftest auth-credentials-test
  (testing "Properly authenticated request"
    (with-redefs [auth/token->credentials (constantly {:aud      "test"
                                                       :username "sandy"})]
      (let [valid-request {:headers {"content-type"  "application/json"
                                     "authorization" "Bearer token"}}]
        (is (map-contains? (auth/auth-credentials valid-request)
                           {:aud      "test"
                            :username "sandy"})))))

  (testing "Bad request"
    (with-redefs [auth/token->credentials (constantly {:aud      "test"
                                                       :username "sandy"})]
      (let [invalid-request {:headers {"authorization" "Digest token"}}]
        (is (nil? (auth/auth-credentials invalid-request)))))))

(deftest wrap-auth-test
  (let [handler         (constantly
                          (res/response "Here is your response. Share it with all your friends."))
        wrapped-handler (auth/wrap-auth handler)
        user-creds      {:aud      "test"
                         :username "sandy"}]

    (testing "Handle an authenticated request"
      (with-redefs [auth/token->credentials (constantly user-creds)
                    config/get-config       (constantly "test")]
        (let [valid-request {:headers {"authorization" "Bearer token"}}]
          (is (= 200
                 (:status (wrapped-handler valid-request)))))))

    (testing "Handle an unauthenticated request"
      (with-redefs [auth/token->credentials (constantly nil)
                    config/get-config       (constantly "test")]
        (let [request {:headers {"authorization" "Bearer token"}}]
          (is (= 403
                 (:status (wrapped-handler request)))))))))
