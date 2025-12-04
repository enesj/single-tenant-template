(ns app.backend.routes.admin.login-events-test
  "Tests for login events monitoring API routes.
   
   Tests cover:
   - Listing login events with filtering
   - Handler creation validation"
  (:require
    [app.backend.routes.admin.login-events :as login-events]
    [app.backend.services.monitoring.login-events :as login-monitoring]
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
(def test-event-id (java.util.UUID/randomUUID))
(def test-user-id (java.util.UUID/randomUUID))

(def mock-admin
  {:id test-admin-id
   :email "admin@example.com"
   :full_name "Test Admin"
   :role "owner"})

(def mock-login-event
  {:id test-event-id
   :principal_type "user"
   :principal_id test-user-id
   :email "user@example.com"
   :ip_address "192.168.1.1"
   :user_agent "Test Browser"
   :success true
   :created_at (java.time.Instant/now)})

(def mock-login-events
  [mock-login-event
   {:id (java.util.UUID/randomUUID)
    :principal_type "admin"
    :principal_id test-admin-id
    :email "admin@example.com"
    :ip_address "192.168.1.2"
    :user_agent "Admin Browser"
    :success true
    :created_at (java.time.Instant/now)}])

;; ============================================================================
;; Handler Creation Tests
;; ============================================================================

(deftest handler-creation-test
  (testing "get-login-events-handler returns a function"
    (let [db (h/mock-db)
          handler (login-events/get-login-events-handler db)]
      (is (fn? handler))))

  (testing "delete-login-event-handler returns a function"
    (let [db (h/mock-db)
          handler (login-events/delete-login-event-handler db)]
      (is (fn? handler))))

  (testing "bulk-delete-login-events-handler returns a function"
    (let [db (h/mock-db)
          handler (login-events/bulk-delete-login-events-handler db)]
      (is (fn? handler)))))

;; ============================================================================
;; Get Login Events Tests (with mocked service)
;; ============================================================================

(deftest get-login-events-handler-test
  (testing "get-login-events returns events list"
    (let [db (h/mock-db)
          handler (login-events/get-login-events-handler db)
          request (h/mock-admin-request :get "/admin/api/login-events" mock-admin {})]
      (with-redefs [login-monitoring/list-login-events (constantly mock-login-events)]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (vector? (:events body)))
          (is (= 2 (count (:events body))))))))

  (testing "get-login-events filters by principal-type"
    (let [db (h/mock-db)
          handler (login-events/get-login-events-handler db)
          request (h/mock-admin-request :get "/admin/api/login-events" mock-admin
                    {:params {:principal-type "user"}})]
      (with-redefs [login-monitoring/list-login-events
                    (fn [_db opts]
                      (is (= :user (:principal-type opts)))
                      [mock-login-event])]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (= 1 (count (:events body))))))))

  (testing "get-login-events filters by success status"
    (let [db (h/mock-db)
          handler (login-events/get-login-events-handler db)
          request (h/mock-admin-request :get "/admin/api/login-events" mock-admin
                    {:params {:success "true"}})]
      (with-redefs [login-monitoring/list-login-events
                    (fn [_db opts]
                      (is (true? (:success? opts)))
                      mock-login-events)]
        (let [response (handler request)]
          (is (= 200 (:status response)))))))

  (testing "get-login-events respects pagination"
    (let [db (h/mock-db)
          handler (login-events/get-login-events-handler db)
          request (h/mock-admin-request :get "/admin/api/login-events" mock-admin
                    {:params {:limit "50" :offset "10"}})]
      (with-redefs [login-monitoring/list-login-events
                    (fn [_db opts]
                      (is (= 50 (:limit opts)))
                      (is (= 10 (:offset opts)))
                      [])]
        (let [response (handler request)]
          (is (= 200 (:status response))))))))

;; ============================================================================
;; Service Function Tests (can be tested without mocking JDBC)
;; ============================================================================

(deftest login-monitoring-service-test
  (testing "list-login-events function exists"
    (is (fn? login-monitoring/list-login-events)))

  (testing "record-login-event! function exists"
    (is (fn? login-monitoring/record-login-event!)))

  (testing "count-recent-login-events function exists"
    (is (fn? login-monitoring/count-recent-login-events)))

  (testing "get-login-history function exists"
    (is (fn? login-monitoring/get-login-history))))
