(ns app.backend.routes.api-test
  "Tests for public API endpoints.
   
   Tests metrics, config, and health endpoints."
  (:require
    [app.backend.routes :as routes]
    [app.backend.routes.admin-api :as admin-api]
    [app.backend.services.admin.dashboard :as admin-dashboard]
    [app.backend.services.monitoring.login-events :as login-monitoring]
    [app.backend.test-helpers :as h]
    [app.backend.webserver :as webserver]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]]
    [ring.mock.request :as mock]))

;; ============================================================================
;; Metrics Endpoint Tests
;; ============================================================================

(deftest metrics-endpoint-test
  (testing "metrics endpoint returns JSON with expected structure"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/api/v1/metrics"))]
      (is (h/ok? resp))
      (is (h/json-content-type? resp))
      (let [body (h/parse-json-body resp)]
        (is (= "ok" (:status body)))
        (is (contains? body :login-metrics)))))

  (testing "metrics endpoint includes login metrics"
    (with-redefs [login-monitoring/count-recent-login-events 
                  (fn [_ _] 42)]
      (let [handler (h/build-handler)
            resp (handler (mock/request :get "/api/v1/metrics"))
            body (h/parse-json-body resp)]
        (is (= 42 (get-in body [:login-metrics :last-24h :total])))))))

;; ============================================================================
;; Config Endpoint Tests
;; ============================================================================

(deftest config-endpoint-test
  (testing "config endpoint returns 200"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/api/v1/config"))]
      (is (h/ok? resp))))

  (testing "config endpoint returns models data"
    (let [service-container (h/stub-service-container 
                              {:models-data {:items {:fields {:id :uuid
                                                              :name :string}}}})
          handler (h/build-handler service-container)
          resp (handler (mock/request :get "/api/v1/config"))]
      (is (h/ok? resp))
      (let [body (h/parse-json-body resp)]
        (is (map? body))))))

;; ============================================================================
;; Home Route Tests
;; ============================================================================

(deftest home-route-test
  (testing "home route returns HTML"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/"))]
      (is (h/ok? resp))
      (is (h/html-content-type? resp))))

  (testing "home route HTML contains expected content"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/"))
          body (h/slurp-body resp)]
      (is (or (clojure.string/includes? body "<!DOCTYPE")
              (clojure.string/includes? body "<html")
              (clojure.string/includes? body "<")
              (clojure.string/includes? body "Test route works"))))))

;; ============================================================================
;; Auth Endpoint Tests  
;; ============================================================================

(deftest auth-login-endpoint-test
  (testing "login endpoint accepts POST"
    (let [handler (h/build-handler)
          resp (handler (h/json-request :post "/api/v1/auth/login" 
                                        {:email "test@example.com"
                                         :password "password123"}))]
      (is (h/ok? resp))))

  (testing "login endpoint uses stub handler"
    (let [handler (h/build-handler)
          resp (handler (h/json-request :post "/api/v1/auth/login"))
          body (h/parse-json-body resp)]
      (is (:ok body))
      (is (:has-service-container body)))))

;; ============================================================================
;; API Version Header Tests
;; ============================================================================

(deftest api-version-header-test
  (testing "API endpoints include version header"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/api/v1/metrics"))]
      (is (= "v1" (get-in resp [:headers "X-API-Version"])))))

  (testing "version header present on config endpoint"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/api/v1/config"))]
      (is (= "v1" (get-in resp [:headers "X-API-Version"]))))))

;; ============================================================================
;; 404 Tests
;; ============================================================================

(deftest not-found-test
  (testing "unknown API path returns 404"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/api/v1/unknown-endpoint"))]
      (is (h/not-found? resp))))

  (testing "malformed API path returns 404"
    (let [handler (h/build-handler)
          resp (handler (mock/request :get "/api/v999/metrics"))]
      (is (h/not-found? resp)))))
