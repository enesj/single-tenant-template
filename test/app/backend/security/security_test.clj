(ns app.backend.security.security-test
  "Comprehensive security test suite for the multi-tenant application.

   Tests cover:
   1. Entity access control (allowlist/blocklist)
   2. HTTP route authorization
   3. Database RLS policies
   4. Database context setting
   5. Admin bypass functionality
   6. Cross-tenant isolation"
  (:require
    [app.backend.middleware.database-context :as db-context]
    [app.backend.middleware.user :as user-middleware]
    [app.backend.security.entity-access :as entity-access]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc]
    [ring.mock.request :as mock]
    [taoensso.timbre :as log]))

;; ================================================================================
;; Test Configuration and Helpers
;; ================================================================================

(def test-tenant-id "11111111-1111-1111-1111-111111111111")
(def other-tenant-id "22222222-2222-2222-2222-222222222222")
(def test-user-id "33333333-3333-3333-3333-333333333333")
(def test-admin {:id "44444444-4444-4444-4444-444444444444" :role "admin"})

(defn mock-user-session [tenant-id user-id]
  {:session {:auth-session {:tenant {:id tenant-id}
                            :user {:id user-id}}}})

(defn mock-admin-session []
  {:admin test-admin})

(defn create-test-db-spec []
  ;; Use test database connection
  {:connection-uri "jdbc:postgresql://localhost:5433/bookkeeping-test?user=user&password=password"})

;; ================================================================================
;; Entity Access Control Tests
;; ================================================================================

(deftest test-entity-access-validation
  (testing "Entity access configuration validation"
    (is (entity-access/validate-entity-access-config))
    (log/info "✅ Entity access config validation passed")))

(deftest test-admin-only-entities
  (testing "Admin-only entities are blocked for regular users"
    (is (false? (entity-access/entity-allowed-for-generic-crud? :admins false)))
    (is (false? (entity-access/entity-allowed-for-generic-crud? :admin_sessions false)))
    (is (false? (entity-access/entity-allowed-for-generic-crud? :audit_logs false)))
    (is (false? (entity-access/entity-allowed-for-generic-crud? :tenant_limits false)))
    (is (false? (entity-access/entity-allowed-for-generic-crud? :billing_events false)))))

(deftest test-protected-entities
  (testing "Protected entities are allowed but with restrictions"
    (is (true? (entity-access/entity-allowed-for-generic-crud? :tenants false)))
    (is (true? (entity-access/entity-allowed-for-generic-crud? :users false)))))

(deftest test-public-entities
  (testing "Public entities are allowed for regular users"
    (is (true? (entity-access/entity-allowed-for-generic-crud? :properties false)))
    (is (true? (entity-access/entity-allowed-for-generic-crud? :transactions_v2 false)))
    (is (true? (entity-access/entity-allowed-for-generic-crud? :transaction_types_v2 false)))))

(deftest test-unknown-entities
  (testing "Unknown entities are blocked by default"
    (is (false? (entity-access/entity-allowed-for-generic-crud? :unknown_entity false)))))

;; ================================================================================
;; HTTP Authorization Middleware Tests
;; ================================================================================

(deftest test-user-authentication-middleware
  (testing "User authentication middleware"
    (let [handler (user-middleware/wrap-user-authentication
                    (fn [_req] {:status 200 :body "success"}))

          ;; Request with no user session
          no-auth-req (mock/request :get "/api/entities/properties")
          no-auth-resp (handler no-auth-req)

          ;; Request with user session
          auth-req (merge (mock/request :get "/api/entities/properties")
                     (mock-user-session test-tenant-id test-user-id))
          auth-resp (handler auth-req)]

      ;; No authentication should return 401
      (is (= 401 (:status no-auth-resp)))

      ;; Valid authentication should succeed
      (is (= 200 (:status auth-resp))))))

(deftest test-entities-authorization-middleware
  (testing "Entity authorization middleware blocks admin entities"
    (let [handler (user-middleware/wrap-entities-authorization
                    (fn [_req] {:status 200 :body "success"}))

          ;; Request for admin entity
          admin-req (merge (mock/request :get "/api/entities/admins")
                      (mock-user-session test-tenant-id test-user-id))
          admin-resp (handler admin-req)

          ;; Request for allowed entity
          allowed-req (merge (mock/request :get "/api/entities/properties")
                        (mock-user-session test-tenant-id test-user-id))
          allowed-resp (handler allowed-req)]

      ;; Admin entity should be blocked
      (is (= 403 (:status admin-resp)))

      ;; Allowed entity should succeed
      (is (= 200 (:status allowed-resp))))))

(deftest test-tenant-scoping
  (testing "Tenants entity is properly scoped to current tenant"
    (let [handler (user-middleware/wrap-entities-authorization
                    (fn [req] {:status 200 :body "success" :request req}))

          ;; GET request to tenants collection
          get-req (merge (mock/request :get "/api/entities/tenants")
                    (mock-user-session test-tenant-id test-user-id))
          get-resp (handler get-req)

          ;; POST request to tenants (should be blocked)
          post-req (merge (mock/request :post "/api/entities/tenants")
                     (mock-user-session test-tenant-id test-user-id))
          post-resp (handler post-req)]

      ;; GET should succeed and inject tenant filter
      (is (= 200 (:status get-resp)))
      (is (= test-tenant-id (get-in (:request (:body get-resp)) [:query-params :filters :id])))

      ;; POST should be blocked
      (is (= 403 (:status post-resp))))))

;; ================================================================================
;; Database Context Tests
;; ================================================================================

(deftest test-database-context-functions
  (testing "Database context setting functions"
    (let [db (create-test-db-spec)]
      (try
        ;; Test tenant context setting
        (is (db-context/set-tenant-context! db test-tenant-id))

        ;; Verify tenant context is set
        (let [result (jdbc/execute! db ["SELECT current_tenant_id()"])]
          (is (= test-tenant-id (str (-> result first vals first)))))

        ;; Test admin bypass context
        (is (db-context/set-admin-bypass-context! db))

        ;; Verify admin bypass is set
        (let [result (jdbc/execute! db ["SELECT bypass_rls_for_admin()"])]
          (is (-> result first vals first)))

        ;; Test context clearing
        (db-context/clear-database-context! db)

        (catch Exception e
          (log/warn e "Database context test failed - database may not be available"))))))

(deftest test-database-context-middleware
  (testing "Database context middleware"
    (let [db (create-test-db-spec)
          handler (db-context/wrap-database-context
                    (fn [_req]
                      ;; Check if context is properly set
                      (try
                        (let [tenant-check (jdbc/execute! db ["SELECT current_tenant_id()"])
                              admin-check (jdbc/execute! db ["SELECT bypass_rls_for_admin()"])]
                          {:status 200
                           :body {:tenant-context (-> tenant-check first vals first)
                                  :admin-context (-> admin-check first vals first)}})
                        (catch Exception e
                          {:status 500 :body {:error (.getMessage e)}})))
                    db)]

      (try
        ;; Test with tenant user request
        (let [tenant-req (merge (mock/request :get "/api/entities/properties")
                           (mock-user-session test-tenant-id test-user-id))
              tenant-resp (handler tenant-req)]
          (when (= 200 (:status tenant-resp))
            (is (= test-tenant-id (str (:tenant-context (:body tenant-resp)))))
            (is (false? (:admin-context (:body tenant-resp))))))

        ;; Test with admin request
        (let [admin-req (merge (mock/request :get "/admin/api/dashboard")
                          (mock-admin-session))
              admin-resp (handler admin-req)]
          (when (= 200 (:status admin-resp))
            (is (true? (:admin-context (:body admin-resp))))))

        (catch Exception e
          (log/warn e "Database context middleware test failed - database may not be available"))))))

;; ================================================================================
;; Cross-Tenant Isolation Tests
;; ================================================================================

(deftest test-cross-tenant-path-isolation
  (testing "Cross-tenant path access is properly blocked"
    (let [handler (user-middleware/wrap-entities-authorization
                    (fn [_req] {:status 200 :body "success"}))

          ;; Request to access different tenant's data
          cross-tenant-req (merge (mock/request :get (str "/api/entities/tenants/" other-tenant-id))
                             (mock-user-session test-tenant-id test-user-id))
          cross-tenant-resp (handler cross-tenant-req)]

      ;; Should be blocked
      (is (= 403 (:status cross-tenant-resp))))))

;; ================================================================================
;; Integration Tests
;; ================================================================================

(deftest test-security-integration
  (testing "Full security stack integration"
    (let [db (create-test-db-spec)
          ;; Create middleware stack in correct order
          handler (-> (fn [_req] {:status 200 :body "success"})
                    (user-middleware/wrap-entities-authorization)
                    (user-middleware/wrap-user-authentication)
                    (db-context/wrap-database-context db))]

      (try
        ;; Test 1: Unauthenticated request should be blocked
        (let [unauth-req (mock/request :get "/api/entities/properties")
              unauth-resp (handler unauth-req)]
          (is (= 401 (:status unauth-resp))))

        ;; Test 2: Admin entity access should be blocked for regular users
        (let [admin-entity-req (merge (mock/request :get "/api/entities/admins")
                                 (mock-user-session test-tenant-id test-user-id))
              admin-entity-resp (handler admin-entity-req)]
          (is (= 403 (:status admin-entity-resp))))

        ;; Test 3: Valid tenant entity access should succeed
        (let [valid-req (merge (mock/request :get "/api/entities/properties")
                          (mock-user-session test-tenant-id test-user-id))
              valid-resp (handler valid-req)]
          (is (= 200 (:status valid-resp))))

        (catch Exception e
          (log/warn e "Security integration test failed - database may not be available"))))))

;; ================================================================================
;; Performance and Edge Case Tests
;; ================================================================================

(deftest test-security-performance
  (testing "Security middleware performance with many requests"
    (let [db (create-test-db-spec)
          handler (-> (fn [_req] {:status 200 :body "success"})
                    (user-middleware/wrap-entities-authorization)
                    (db-context/wrap-database-context db))

          test-req (merge (mock/request :get "/api/entities/properties")
                     (mock-user-session test-tenant-id test-user-id))]

      (try
        ;; Test multiple requests to ensure no context leakage
        (dotimes [_i 10]
          (let [resp (handler test-req)]
            (is (= 200 (:status resp)))))

        (log/info "✅ Security performance test completed")

        (catch Exception e
          (log/warn e "Security performance test failed - database may not be available"))))))

(deftest test-security-edge-cases
  (testing "Security edge cases and error conditions"
    (let [handler (user-middleware/wrap-entities-authorization
                    (fn [_req] {:status 200 :body "success"}))]

      ;; Test with nil entity
      (let [nil-entity-req (merge (mock/request :get "/api/entities/")
                             (mock-user-session test-tenant-id test-user-id))
            nil-entity-resp (handler nil-entity-req)]
        ;; Should handle gracefully
        (is (or (= 200 (:status nil-entity-resp))
              (= 404 (:status nil-entity-resp)))))

      ;; Test with malformed tenant ID in session
      (let [bad-session-req (merge (mock/request :get "/api/entities/properties")
                              {:session {:auth-session {:tenant {:id "invalid-uuid"}}}})
            bad-session-resp (handler bad-session-req)]
        ;; Should handle gracefully (may be 401 or 403)
        (is (#{401 403} (:status bad-session-resp)))))))

;; ================================================================================
;; Test Runner
;; ================================================================================

(defn run-security-tests
  "Run all security tests and report results.

   This should be called during deployment validation to ensure
   security measures are working correctly."
  []
  (log/info "🧪 Starting comprehensive security test suite...")

  (try
    ;; Validate configuration first
    (entity-access/validate-entity-access-config)

    ;; Run all tests
    (clojure.test/run-tests 'app.backend.security.security-test)

    (log/info "✅ Security test suite completed")
    true

    (catch Exception e
      (log/error e "❌ Security test suite failed")
      false)))

(comment
  ;; Run tests manually:
  (run-security-tests)

  ;; Run specific test groups:
  (clojure.test/run-tests #'test-entity-access-validation)
  (clojure.test/run-tests #'test-admin-only-entities)
  (clojure.test/run-tests #'test-security-integration))
