(ns time-tracker.test-helpers
  (:require [time-tracker.auth.test-helpers :as auth.helpers]
            [org.httpkit.client :as http]
            [cheshire.core :as json]))

(defn http-request
  ([method url google-id] (http-request method url google-id nil))
  ([method url google-id body]
   (let [params       (merge {:url url
                              :method method
                              :headers (merge (auth.helpers/fake-login-headers google-id)
                                              {"Content-Type" "application/json"})
                              :as :text}
                             (if body {:body (json/encode body)}))
         response     @(http/request params)
         decoded-body (json/decode (:body response))]
     (assoc response :body decoded-body))))
