(ns app.shared.http-test
  (:require
    [app.shared.http :as http]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]]))

(defn- parse-json [s]
  (json/parse-string s true))

(deftest encode-json-body-encodes-non-string-body
  (testing "encode-json-body turns a data :body into a JSON string and preserves status"
    (let [resp (http/encode-json-body (http/json-response 201 {:ok true :n 1}))]
      (is (= 201 (:status resp)))
      (is (string? (:body resp)))
      (is (= {:ok true :n 1} (parse-json (:body resp))))
      (is (re-find #"application/json" (get-in resp [:headers http/header-content-type] ""))))))

(deftest encode-json-body-noops-for-string-body
  (testing "encode-json-body is a no-op when :body is already a string"
    (let [resp (http/encode-json-body {:status 200 :headers {} :body "{\"ok\":true}"})]
      (is (= "{\"ok\":true}" (:body resp))))))

(deftest json-string-response-builds-string-body
  (testing "json-string-response builds a response with string body"
    (let [resp (http/json-string-response 200 {:ok true})]
      (is (= 200 (:status resp)))
      (is (string? (:body resp)))
      (is (= {:ok true} (parse-json (:body resp)))))))

(deftest error-string-response-builds-string-body
  (testing "error-string-response mirrors error-response, but JSON-encodes the body"
    (let [resp (http/error-string-response 400 "Nope" {:reason "bad"})]
      (is (= 400 (:status resp)))
      (is (string? (:body resp)))
      (is (= {:error "Nope" :details {:reason "bad"}} (parse-json (:body resp)))))))
