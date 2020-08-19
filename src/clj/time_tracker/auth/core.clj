(ns time-tracker.auth.core
  (:require [time-tracker.util :as util]
            [time-tracker.config :as config]
            [time-tracker.web.util :as web-util]
            [mount.core :refer [defstate]])
  (:import [com.google.api.client.googleapis.auth.oauth2 GoogleIdTokenVerifier GoogleIdTokenVerifier$Builder]
           [com.google.api.client.googleapis.javanet GoogleNetHttpTransport]
           [com.google.api.client.json.jackson2 JacksonFactory]))

(defstate google-id-token-verifier
  :start (-> (GoogleIdTokenVerifier$Builder. (GoogleNetHttpTransport/newTrustedTransport)
                                             (JacksonFactory.))
             (.setAudience [(config/get-config :google-client-id)])
             (.build))
  :stop nil)

(defn token->credentials
  [^String token]
  (some->> (.verify ^GoogleIdTokenVerifier google-id-token-verifier token)
           (.getPayload)
           (into {})
           (util/transform-keys util/hyphenize)))

(defn token-from-headers
  "Extracts the token from a Ring header map, if present.
  See: https://jwt.io/introduction/"
  [ring-headers]
  (when-let [header-value (get ring-headers "authorization")]
    (let [[scheme & rest] (clojure.string/split header-value #" ")
          token (clojure.string/join " " rest)]
      (when (= "Bearer" scheme)
        token))))

(defn auth-credentials
  "Gets a map of credentials from a Ring request.
  A list of client-ids is needed to validate the JWT."
  [request]
  (when-let [token (token-from-headers (:headers request))]
    (token->credentials token)))

(defn wrap-auth
  "Middleware to verify Google authentication"
  [handler]
  (fn [request]
    (if-let [user-information (auth-credentials request)]
      (handler (assoc request :credentials user-information))
      web-util/error-forbidden)))
