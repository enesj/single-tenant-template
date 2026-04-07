(ns app.backend.routes.admin.login-events-test
  "Tests for login events monitoring API routes.

   Tests cover:
   - Listing login events with filtering
   - Handler creation validation"
  (:require
    [app.backend.test-helpers :as h]
    [app.template.backend.routes.admin.login-events :as login-events]
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.services.monitoring.login-events :as login-monitoring]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [honey.sql :as hsql]
    [next.jdbc]))

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

(deftest count-login-events-uses-db-as-first-arg
  (testing "count-login-events passes the datasource before SQL params"
    (let [db ::mock-db
          execute-args (atom nil)]
      (with-redefs [next.jdbc/execute-one! (fn [& args]
                                             (reset! execute-args args)
                                             {:total 7})]
        (is (= 7 (login-monitoring/count-login-events db {})))
        (is (= db (first @execute-args)))
        (is (vector? (second @execute-args)))
        (is (string? (ffirst (rest @execute-args))))))))

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
    (is (fn? login-monitoring/get-login-history)))

  (testing "list-login-events exposes pseudonymous principal refs without raw email"
    (let [principal-id (java.util.UUID/randomUUID)
          row {:id (java.util.UUID/randomUUID)
               :principal_type "admin"
               :principal_id principal-id
               :success true
               :reason nil
               :ip "127.0.0.1"
               :user_agent "Test Browser"
               :created_at (java.time.Instant/parse "2026-01-01T00:00:00Z")
               :admin_name nil}
          result (first (with-redefs [hsql/format identity
                                      next.jdbc/execute! (fn [_db _query] [row])]
                          (login-monitoring/list-login-events ::db {:limit 1 :offset 0})))]
      (is (= (email-privacy/admin-ref principal-id) (:principal-ref result)))
      (is (= (:principal-ref result) (:principal-name result)))
      (is (not (contains? result :principal-email)))))

  (testing "successful logins also update the principal last_login_at field"
    (let [calls (atom [])
          user-id (java.util.UUID/randomUUID)]
      (with-redefs [hsql/format identity
                    next.jdbc/execute-one! (fn [_db query]
                                             (swap! calls conj query)
                                             {})]
        (login-monitoring/record-login-event! ::db {:principal-type :user
                                                    :principal-id user-id
                                                    :success true
                                                    :ip "127.0.0.1"
                                                    :user-agent "Test UA"})
        (is (= 2 (count @calls))))
      (is (= :login_events (:insert-into (first @calls))))
      (is (= :users (:update (second @calls))))))

  (testing "failed logins do not update last_login_at"
    (let [calls (atom [])
          user-id (java.util.UUID/randomUUID)]
      (with-redefs [hsql/format identity
                    next.jdbc/execute-one! (fn [_db query]
                                             (swap! calls conj query)
                                             {})]
        (login-monitoring/record-login-event! ::db {:principal-type :user
                                                    :principal-id user-id
                                                    :success false
                                                    :reason "invalid_credentials"
                                                    :ip "127.0.0.1"
                                                    :user-agent "Test UA"})
        (is (= 1 (count @calls))))
      (is (= :login_events (:insert-into (first @calls)))))))
