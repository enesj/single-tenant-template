(ns app.backend.routes.admin.login-events-test
  "Tests for login events monitoring API routes.
   
   Tests cover:
   - Listing login events with filtering
   - Handler creation validation"
  (:require
    [app.template.backend.routes.admin.login-events :as login-events]
    [app.template.backend.services.monitoring.login-events :as login-monitoring]
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
  (testing "get-login-events returns events list with pagination metadata"
    (let [db (h/mock-db)
          handler (login-events/get-login-events-handler db)
          request (h/mock-admin-request :get "/admin/api/login-events" mock-admin {})]
      (with-redefs [login-monitoring/list-login-events-page
                    (fn [_db _opts]
                      {:events mock-login-events
                       :total 2
                       :limit 100
                       :offset 0})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (vector? (:events body)))
          (is (= 2 (count (:events body))))
          (is (= 2 (:total body)))
          (is (= 100 (:limit body)))
          (is (= 0 (:offset body)))))))

  (testing "get-login-events filters by principal-type and success with filter-aware total"
    (let [db (h/mock-db)
          handler (login-events/get-login-events-handler db)
          request (h/mock-admin-request :get "/admin/api/login-events" mock-admin
                    {:params {:principal-type "user"
                              :success "true"
                              :limit "10"
                              :offset "0"}})
          sample-events [{:id (java.util.UUID/randomUUID)
                          :principal-type :user
                          :success true}
                         {:id (java.util.UUID/randomUUID)
                          :principal-type :user
                          :success false}
                         {:id (java.util.UUID/randomUUID)
                          :principal-type :admin
                          :success true}]]
      (with-redefs [login-monitoring/list-login-events-page
                    (fn [_db opts]
                      (is (= :user (:principal-type opts)))
                      (is (true? (:success? opts)))
                      (is (= 10 (:limit opts)))
                      (is (= 0 (:offset opts)))
                      (let [matches? (fn [event]
                                       (and (= (:principal-type opts) (:principal-type event))
                                         (= (:success? opts) (:success event))))
                            filtered (vec (filter matches? sample-events))]
                        {:events filtered
                         :total (count filtered)
                         :limit (:limit opts)
                         :offset (:offset opts)}))]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (= 1 (count (:events body))))
          (is (= 1 (:total body)))
          (is (= 10 (:limit body)))
          (is (= 0 (:offset body))))))))

;; ============================================================================
;; Service Function Tests (can be tested without mocking JDBC)
;; ============================================================================

(deftest login-monitoring-service-test
  (testing "list-login-events function exists"
    (is (fn? login-monitoring/list-login-events)))

  (testing "list-login-events-page function exists"
    (is (fn? login-monitoring/list-login-events-page)))

  (testing "count-login-events function exists"
    (is (fn? login-monitoring/count-login-events)))

  (testing "record-login-event! function exists"
    (is (fn? login-monitoring/record-login-event!)))

  (testing "count-recent-login-events function exists"
    (is (fn? login-monitoring/count-recent-login-events)))

  (testing "get-login-history function exists"
    (is (fn? login-monitoring/get-login-history))))
