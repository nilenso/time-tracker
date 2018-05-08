(ns time-tracker.auth.core
  (:require [ring.util.response :as res]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [time-tracker.util :refer [from-config] :as util]
            [time-tracker.web.util :as web-util]))

(defn- call-google-tokeninfo-api
  [token]
  (let [{body :body :as response} @(http/get (from-config :google-tokeninfo-url)
                                             {:as :text
                                              :query-params {"id_token" token}})]
    (assoc response :body (json/parse-string body util/hyphenize))))

(defn allowed-hosted-domain?
  [domain]
  (let [allowed-domain (from-config :allowed-hosted-domain)]
    (or (= "*" allowed-domain)
        (= allowed-domain domain))))

(defn validate-body
  [client-ids token-body]
  (and (some #{(:aud token-body)} client-ids)
       (allowed-hosted-domain? (:hd token-body))))

(defn token->credentials
  "Validates a JWT by calling Google's API and by checking the client ID."
  [client-ids token]
  (let [{:keys [status body]} (call-google-tokeninfo-api token)]
    (when (and (= 200 status)
             (some #{(:aud body)} client-ids))
      body)))

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
  [client-ids request]
  (when-let [token (token-from-headers (:headers request))]
    (token->credentials client-ids token)))

(defn wrap-google-authenticated
  "Middleware to verify Google authentication"
  [handler client-ids]
  (fn [request]
    (if-let [user-information (auth-credentials client-ids request)]
      (handler (assoc request :credentials user-information))
      web-util/error-forbidden)))

(def wrap-auth
  #(wrap-google-authenticated % [(from-config :google-client-id)]))
