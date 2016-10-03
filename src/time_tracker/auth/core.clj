(ns time-tracker.auth.core
  (:require [ring.util.response :as res]
            [org.httpkit.client :as http]
            [cheshire.core :as json]

            [time-tracker.config :as config]))

(defn- snake-case->hyphenated-kw
  "In: \"key_string\"
  Out: :key-string"
  [key-string]
  (keyword (clojure.string/replace key-string #"_" "-")))

(defn- call-google-tokeninfo-api
  [token]
  (let [{body :body :as response} @(http/get config/google-tokeninfo-url
                                             {:as :text
                                              :query-params {"id_token" token}})]
    (assoc response :body (json/parse-string body snake-case->hyphenated-kw))))

(defn token->credentials
  "Validates a JWT by calling Google's API and by checking the client ID."
  [client-ids token]
  (let [{:keys [status body]} (call-google-tokeninfo-api token)]
    (if (and (= 200 status)
             (some #{(:aud body)} client-ids))
      body)))

(defn token-from-headers
  "Extracts the token from a Ring header map, if present.
  See: https://jwt.io/introduction/"
  [ring-headers]
  (if-let [header-value (get ring-headers "authorization")]
    (let [[scheme token] (clojure.string/split header-value #" ")]
      (if (= "Bearer" scheme)
        token))))

(defn auth-credentials
  "Gets a map of credentials from a Ring request.
  A list of client-ids is needed to validate the JWT."
  [client-ids request]
  (if-let [token (token-from-headers (:headers request))]
    (token->credentials client-ids token)))

(defn wrap-google-authenticated
  "Middleware to verify Google authentication"
  [client-ids handler]
  (fn [request]
    (if-let [user-information (auth-credentials client-ids request)]
      (handler (assoc request :credentials user-information))
      (-> (res/response "Access Denied")
          (res/status 403)))))
