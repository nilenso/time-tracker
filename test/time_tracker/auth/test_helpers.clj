(ns time-tracker.auth.test-helpers
  (:require [cheshire.core :as json]
            [time-tracker.util :as util]))

(defn fake-token->credentials
  "Fakes core/token->credentials"
  [_ token]
  (let [body (json/decode token util/snake-case->hyphenated-kw)]
    body))

(defn fake-login-headers
  "Fake headers to be used with fake-google-tokeninfo-api"
  [google-id name]
  (let [token (json/encode {:sub  google-id
                            :name name})]
    {"Authorization" (format "Bearer %s" token)}))
