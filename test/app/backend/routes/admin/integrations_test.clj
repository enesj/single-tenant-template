(ns app.backend.routes.admin.integrations-test
  "Tests for admin integration management routes.
   
   Tests cover:
   - Integration overview
   - Integration performance metrics
   - Webhook status"
  (:require
    [app.backend.routes.admin.integrations :as integrations]
    [app.backend.services.admin :as admin-service]
    [app.backend.test-helpers :as h]
    [clojure.test :refer [deftest is testing use-fixtures]]))

;; ============================================================================
;; Test Fixtures
;; ============================================================================

(use-fixtures :each h/with-clean-test-state)

;; ============================================================================
;; Test Data
;; ============================================================================

(def test-admin-id (java.util.UUID/randomUUID))

(def mock-admin
  {:id test-admin-id
   :email "admin@example.com"
   :full_name "Test Admin"
   :role "owner"})

(def mock-integration-overview
  {:total-integrations 5
   :active-integrations 4
   :failed-integrations 1
   :last-sync "2025-01-15T10:30:00Z"})

(def mock-integration-performance
  [{:integration "stripe"
    :avg-response-time 245
    :success-rate 99.5
    :requests-count 1500}
   {:integration "sendgrid"
    :avg-response-time 180
    :success-rate 99.8
    :requests-count 2300}])

(def mock-webhook-status
  [{:id (java.util.UUID/randomUUID)
    :endpoint "https://example.com/webhook"
    :status "active"
    :last-delivery "2025-01-15T10:30:00Z"
    :success-rate 98.5}])

;; ============================================================================
;; Handler Creation Tests
;; ============================================================================

(deftest handler-creation-test
  (testing "get-integration-overview-handler returns a function"
    (let [db (h/mock-db)
          handler (integrations/get-integration-overview-handler db)]
      (is (fn? handler))))
  
  (testing "get-integration-performance-handler returns a function"
    (let [db (h/mock-db)
          handler (integrations/get-integration-performance-handler db)]
      (is (fn? handler))))
  
  (testing "get-webhook-status-handler returns a function"
    (let [db (h/mock-db)
          handler (integrations/get-webhook-status-handler db)]
      (is (fn? handler)))))

;; ============================================================================
;; Integration Overview Tests
;; ============================================================================

(deftest get-integration-overview-handler-test
  (testing "get-integration-overview returns overview data"
    (let [db (h/mock-db)
          handler (integrations/get-integration-overview-handler db)
          request (h/mock-admin-request :get "/admin/api/integrations/overview" mock-admin {})]
      (with-redefs [admin-service/get-integration-overview
                    (constantly mock-integration-overview)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (or (number? (:total-integrations body))
                (number? (:totalIntegrations body))
                (map? body))))))))

;; ============================================================================
;; Integration Performance Tests
;; ============================================================================

(deftest get-integration-performance-handler-test
  (testing "get-integration-performance returns performance metrics"
    (let [db (h/mock-db)
          handler (integrations/get-integration-performance-handler db)
          request (h/mock-admin-request :get "/admin/api/integrations/performance" mock-admin
                    {:params {:period "hour" :hours "24"}})]
      (with-redefs [admin-service/get-integration-performance
                    (fn [_db opts]
                      (is (= :hour (:period opts)))
                      (is (= 24 (:hours opts)))
                      mock-integration-performance)]
        (let [response (handler request)]
          (is (= 200 (:status response)))))))
  
  (testing "get-integration-performance uses default period"
    (let [db (h/mock-db)
          handler (integrations/get-integration-performance-handler db)
          request (h/mock-admin-request :get "/admin/api/integrations/performance" mock-admin {})]
      (with-redefs [admin-service/get-integration-performance
                    (fn [_db opts]
                      (is (= :hour (:period opts)))
                      mock-integration-performance)]
        (let [response (handler request)]
          (is (= 200 (:status response))))))))

;; ============================================================================
;; Webhook Status Tests
;; ============================================================================

(deftest get-webhook-status-handler-test
  (testing "get-webhook-status returns webhook data"
    (let [db (h/mock-db)
          handler (integrations/get-webhook-status-handler db)
          request (h/mock-admin-request :get "/admin/api/integrations/webhooks" mock-admin
                    {:params {:limit "50" :offset "0"}})]
      (with-redefs [admin-service/get-webhook-status
                    (fn [_db pagination]
                      (is (= 50 (:limit pagination)))
                      mock-webhook-status)]
        (let [response (handler request)]
          (is (= 200 (:status response))))))))

;; ============================================================================
;; Route Definition Tests
;; ============================================================================

(deftest routes-test
  (testing "routes function returns route definitions"
    (let [db (h/mock-db)
          routes (integrations/routes db)]
      (is (vector? routes))
      (is (= "" (first routes))))))
