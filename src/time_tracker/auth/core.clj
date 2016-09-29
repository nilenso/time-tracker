(ns time-tracker.auth.core
  (:gen-class)
  (:require [ring.util.response :as res]
            [org.httpkit.client :as http]
            [cheshire.core :as cheshire]))

(defn is-valid?
  "Validates a JWT token by calling Google's API and by checking the client ID."
  [client-ids token]
  (let [url (str "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token="
                 token)
        response (http/get url {:as :text})]
    (if (= 200 (:status @response))
      (let [response-data (cheshire/parse-string (:body @response)
                                                 #(keyword (clojure.string/replace % #"_" "-")))]
        (and (some #{(:aud response-data)} client-ids)
             response-data)))))

(defn authenticated?
  "Is the request Google authenticated? Performs a GET request.
  There must be a header of the form Authorization: Bearer <token>
  The token must then be verified using Google's verification endpoint."
  [client-ids request]
  (if-let [token (-> (get-in request [:headers "authorization"])
                     (clojure.string/split #" ")
                     (#(and (= "Bearer" (first %))
                            (second %))))]
    (is-valid? token)))

(defn wrap-google-authenticated
  "Middleware to verify Google authentication"
  [client-ids handler]
  (fn [request]
    (if-let [user-information (authenticated? client-ids request)]
      (handler (assoc request :credentials user-information))
      (-> (res/response "Access Denied")
          (res/status 403)))))
