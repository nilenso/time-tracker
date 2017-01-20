(ns time-tracker.web.util
  (:require [clojure.spec :as s]
            [ring.util.response :as res]
            [time-tracker.util :as util]
            [clojure.algo.generic.functor :refer [fmap]]
            [time-tracker.web.spec :as web-spec]))

(defn validate-request-body
  [request spec]
  (util/validate-spec (:body request) spec))

(defn error-response
  [status msg]
  (-> (res/response {:error msg})
      (res/status status)))

(def error-forbidden
  (error-response 403 "Forbidden"))

(def error-method-not-allowed
  (error-response 405 "Method not allowed"))

(def error-not-found
  (error-response 404 "Not found"))

(def error-bad-request
  (error-response 400 "Bad request"))

(def error-internal-server-error
  (error-response 500 "Internal Server Error"))

(defn- coerce-epoch-range-params
  [params]
  (try
    (fmap #(Long/parseLong %) params)
    (catch Exception ex
      (throw (ex-info "Validation failed" {:event :validation-failed
                                           :params params})))))

(defn coerce-and-validate-epoch-range
  [params]
  (let [coerced-params (coerce-epoch-range-params params)]
    (util/validate-spec coerced-params ::web-spec/start-end-epoch-params)
    coerced-params))
