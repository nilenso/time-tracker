(ns time-tracker.auth.core-test
  (:require [clojure.test :refer :all]
            [time-tracker.auth.core :as auth]
            [ring.util.response :as res]))

(def bad-google-api-call (constantly {:status 400}))

(defn good-google-api-call
  [credentials]
  (constantly {:status 200
               :body credentials}))


(deftest token->credentials-test
  (testing "Valid token"
    (with-redefs [auth/call-google-tokeninfo-api
                  (good-google-api-call {:aud "test"})]
      (testing "Valid client id"
        (is (= {:aud "test"}
               (auth/token->credentials ["test"] "token"))))

      (testing "Invalid client id"
        (is (nil? (auth/token->credentials ["foo" "goo"] "token"))))))

  (testing "Invalid token"
    (with-redefs [auth/call-google-tokeninfo-api bad-google-api-call]
      (is (nil? (auth/token->credentials ["foo" "goo"] "token"))))))


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
    (with-redefs [auth/call-google-tokeninfo-api
                  (good-google-api-call {:aud "test"
                                         :username "sandy"})]
      (let [valid-request {:headers {"content-type" "application/json"
                                     "authorization" "Bearer token"}}]
        (is (= {:aud "test"
                :username "sandy"}
               (auth/auth-credentials ["test" "test2"] valid-request))))))

  (testing "Bad request"
    (with-redefs [auth/call-google-tokeninfo-api bad-google-api-call]
      (let [invalid-request {:headers {"authorization" "Digest token"}}]
        (is (nil? (auth/auth-credentials ["test" "test2"] invalid-request)))))))


(deftest wrap-google-authenticated-test
  (let [handler (fn [request]
                  (res/response "Here is your response. Share it with all your friends."))
        wrapped-handler (auth/wrap-google-authenticated handler ["test" "test2"])
        user-creds {:aud "test"
                    :username "sandy"}]
    
    (testing "Handle an authenticated request"
      (with-redefs [auth/call-google-tokeninfo-api
                    (good-google-api-call user-creds)]
        (let [valid-request {:headers {"authorization" "Bearer token"}}]
          (is (= 200
                 (:status (wrapped-handler valid-request)))))))

    (testing "Handle an unauthenticated request"
      (with-redefs [auth/call-google-tokeninfo-api bad-google-api-call]
        (let [request {:headers {"authorization" "Bearer token"}}]
          (is (= 403
                 (:status (wrapped-handler request)))))))))
