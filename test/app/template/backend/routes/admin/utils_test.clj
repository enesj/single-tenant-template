(ns app.template.backend.routes.admin.utils-test
  (:require
    [app.template.backend.routes.admin.utils :as utils]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]]))

(defn- parse-body [response]
  (json/parse-string (:body response) true))

(deftest json-response-encodes-body
  (testing "json-response returns a JSON string body with the requested status"
    (let [response (utils/json-response {:ok true} :status 201)]
      (is (= 201 (:status response)))
      (is (string? (:body response)))
      (is (= {:ok true} (parse-body response)))
      (is (re-find #"application/json" (get-in response [:headers "Content-Type"] ""))))))

(deftest error-response-encodes-details
  (testing "error-response includes :details when provided"
    (let [response (utils/error-response "Nope" :status 400 :details {:reason "bad"})]
      (is (= 400 (:status response)))
      (is (= {:error "Nope" :details {:reason "bad"}} (parse-body response))))))

(deftest with-error-handling-passes-through-4xx
  (testing "passes through explicit client errors (status + message)"
    (let [handler (utils/with-error-handling
                    (fn [_]
                      (throw (ex-info "Cannot delete a posted receipt" {:status 409})))
                    "Failed to delete receipt")
          response (handler {})]
      (is (= 409 (:status response)))
      (is (= "Cannot delete a posted receipt" (:error (parse-body response)))))))

(deftest with-error-handling-hides-unexpected-errors
  (testing "does not leak exception messages for unexpected errors"
    (let [handler (utils/with-error-handling
                    (fn [_]
                      (throw (Exception. "Database is on fire")))
                    "Failed to delete receipt")
          response (handler {})]
      (is (= 500 (:status response)))
      (is (= "Failed to delete receipt" (:error (parse-body response)))))))
