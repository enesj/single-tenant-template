(ns app.template.frontend.api.http-test
  (:require
    [app.template.frontend.api :as api]
    [app.template.frontend.api.http :as http]
    [cljs.test :refer-macros [deftest is testing]]))

(deftest test-api-request
  (testing "api-request creates correct request configuration"
    (let [config {:method :get
                  :uri "/api/test"
                  :on-success [:success]
                  :on-failure [:failure]}
          result (http/api-request config)]
      (is (= :get (:method result)))
      (is (= "/api/test" (:uri result)))
      (is (= [:success] (:on-success result)))
      (is (= [:failure] (:on-failure result)))
      (is (some? (:format result)))
      (is (some? (:response-format result)))))

  (testing "api-request preserves additional options"
    (let [config {:method :post
                  :uri "/api/test"
                  :params {:foo "bar"}
                  :timeout 1000
                  :on-success [:success]
                  :on-failure [:failure]}
          result (http/api-request config)]
      (is (= {:foo "bar"} (:params result)))
      (is (= 1000 (:timeout result))))))

(deftest test-get-entity
  (testing "get-entity creates correct GET request"
    (let [config {:entity-name "users"
                  :id 123
                  :on-success [:success]
                  :on-failure [:failure]}
          result (http/get-entity config)]
      (is (= :get (:method result)))
      (is (= (api/entity-endpoint "users" 123) (:uri result)))
      (is (= [:success] (:on-success result)))
      (is (= [:failure] (:on-failure result))))))

(deftest test-get-entities
  (testing "get-entities creates correct GET request for list"
    (let [config {:entity-name "users"
                  :on-success [:success]
                  :on-failure [:failure]}
          result (http/get-entities config)]
      (is (= :get (:method result)))
      (is (= (api/entity-endpoint "users") (:uri result)))
      (is (= [:success] (:on-success result)))
      (is (= [:failure] (:on-failure result))))))

(deftest test-create-entity
  (testing "create-entity creates correct POST request"
    (let [config {:entity-name "users"
                  :data {:name "John" :email "john@example.com"}
                  :on-success [:success]
                  :on-failure [:failure]}
          result (http/create-entity config)]
      (is (= :post (:method result)))
      (is (= (api/entity-endpoint "users") (:uri result)))
      (is (= {:name "John" :email "john@example.com"} (:params result)))
      (is (= [:success] (:on-success result)))
      (is (= [:failure] (:on-failure result))))))

(deftest test-update-entity
  (testing "update-entity creates correct PUT request"
    (let [config {:entity-name "users"
                  :id 123
                  :data {:name "John Updated"}
                  :on-success [:success]
                  :on-failure [:failure]}
          result (http/update-entity config)]
      (is (= :put (:method result)))
      (is (= (api/entity-endpoint "users" 123) (:uri result)))
      (is (= {:name "John Updated"} (:params result)))
      (is (= [:success] (:on-success result)))
      (is (= [:failure] (:on-failure result))))))

(deftest test-delete-entity
  (testing "delete-entity creates correct DELETE request"
    (let [config {:entity-name "users"
                  :id 123
                  :on-success [:success]
                  :on-failure [:failure]}
          result (http/delete-entity config)]
      (is (= :delete (:method result)))
      (is (= (api/entity-endpoint "users" 123) (:uri result)))
      (is (= [:success] (:on-success result)))
      (is (= [:failure] (:on-failure result))))))

(deftest test-extract-error-message
  (testing "extract-error-message extracts error from various response formats"
    ;; Direct error message
    (is (= "Something went wrong"
          (http/extract-error-message {:error "Something went wrong"})))

    ;; Nested response error
    (is (= "Server error"
          (http/extract-error-message {:response {:error "Server error"}})))

    ;; Status text fallback
    (is (= "Bad Request"
          (http/extract-error-message {:status-text "Bad Request"})))

    ;; Default fallback
    (is (= "An error occurred"
          (http/extract-error-message {})))

    ;; Nil input
    (is (= "An error occurred"
          (http/extract-error-message nil)))))

(deftest test-http-method-helpers
  (testing "GET request helper"
    (let [result (http/get-request {:uri "/test" :on-success [:ok]})]
      (is (= :get (:method result)))
      (is (= "/test" (:uri result)))))

  (testing "POST request helper"
    (let [result (http/post-request {:uri "/test" :params {:a 1}})]
      (is (= :post (:method result)))
      (is (= {:a 1} (:params result)))))

  (testing "PUT request helper"
    (let [result (http/put-request {:uri "/test" :params {:b 2}})]
      (is (= :put (:method result)))
      (is (= {:b 2} (:params result)))))

  (testing "DELETE request helper"
    (let [result (http/delete-request {:uri "/test"})]
      (is (= :delete (:method result))))))
