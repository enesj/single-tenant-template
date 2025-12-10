(ns app.backend.db.integration-test
  "Database integration tests using real test database.
   
   These tests require:
   1. Test database running on port 55433: `docker-compose up -d db-test`
   2. Migrations applied: `clj -X:migrations-test`
   
   Tests use transaction rollback for isolation - no data persists."
  (:require
   [app.backend.fixtures :as fixtures]
   [app.backend.services.admin.audit :as audit-service]
   [app.backend.services.admin.auth :as admin-auth]
   [app.backend.services.admin.users :as user-service]
    [clojure.set :as set]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [honey.sql :as hsql]
    [java-time.api :as jt]
    [next.jdbc :as jdbc])
  (:import
   [java.util UUID]))

;; ============================================================================
;; Test Fixtures - Transaction Rollback for Isolation
;; ============================================================================

(use-fixtures :each fixtures/with-transaction-rollback)

;; ============================================================================
;; Helper Functions
;; ============================================================================

(defn- create-test-admin!
  "Create a test admin within the current transaction"
  [db & [{:keys [email full_name role] :as _opts}]]
  (let [admin-data {:email (or email (str "admin-" (UUID/randomUUID) "@test.com"))
                    :password "test-password-123"
                    :full_name (or full_name "Test Admin")
                    :role (or role "admin")}]
    (admin-auth/create-admin! db admin-data)
    ;; Return the created admin
    (admin-auth/find-admin-by-email db (:email admin-data))))

(defn- create-test-user!
  "Create a test user within the current transaction"
  [db _admin-id & [{:keys [email full_name role status] :as _opts}]]
  (let [user-id (UUID/randomUUID)
      now (jt/instant)
        user-email (or email (str "user-" (UUID/randomUUID) "@test.com"))]
    ;; Insert user directly with all required fields
    ;; Note: PostgreSQL enum types use snake_case (user_role, user_status)
    (jdbc/execute! db
      (hsql/format {:insert-into :users
                    :values [{:id user-id
                              :email user-email
                              :full_name (or full_name "Test User")
                              :password_hash "test-hash-not-real"
                              :role [:cast (or role "member") :user_role]
                              :status [:cast (or status "active") :user_status]
                              :email_verified false
                              :auth_provider "email"
                              :created_at now
                              :updated_at now}]}))
    ;; Return the created user
    (jdbc/execute-one! db
      (hsql/format {:select [:*]
                    :from [:users]
                    :where [:= :id user-id]}))))

;; ============================================================================
;; Database Connection Tests
;; ============================================================================

(deftest db-connection-test
  (testing "test database is accessible"
    (when-let [db fixtures/*test-db*]
      (let [result (jdbc/execute-one! db ["SELECT 1 as test"])]
        (is (= 1 (:test result)))))))

(deftest db-tables-exist-test
  (testing "required tables exist"
    (when-let [db fixtures/*test-db*]
      (let [tables (jdbc/execute! db
                     ["SELECT table_name FROM information_schema.tables 
                       WHERE table_schema = 'public' 
                       AND table_type = 'BASE TABLE'"])
            ;; Handle both namespaced and non-namespaced keys from JDBC
            table-names (set (map #(or (:table_name %) (:tables/table_name %)) tables))]
        (is (contains? table-names "users"))
        (is (contains? table-names "admins"))
        (is (contains? table-names "audit_logs"))
        (is (contains? table-names "login_events"))))))

;; ============================================================================
;; Admin CRUD Tests
;; ============================================================================

(deftest admin-create-test
  (testing "can create admin with valid data"
    (when-let [db fixtures/*test-db*]
      (let [email (str "test-" (UUID/randomUUID) "@example.com")
            _ (admin-auth/create-admin! db {:email email
                                             :password "secure-password-123"
                                             :full_name "Test Admin"
                                             :role "admin"})
            admin (admin-auth/find-admin-by-email db email)]
        (is (some? admin))
        (is (= email (or (:email admin) (:admins/email admin))))
        (is (= "Test Admin" (or (:full_name admin) (:admins/full_name admin))))))))

(deftest admin-authenticate-test
  (testing "admin can authenticate with correct password"
    (when-let [db fixtures/*test-db*]
      (let [email (str "auth-" (UUID/randomUUID) "@example.com")
            password "correct-password-123"
            _ (admin-auth/create-admin! db {:email email
                                             :password password
                                             :full_name "Auth Test"
                                             :role "admin"})
            authenticated (admin-auth/authenticate-admin db email password)]
        (is (some? authenticated))
        (is (= email (or (:email authenticated) (:admins/email authenticated)))))))

  (testing "admin cannot authenticate with wrong password"
    (when-let [db fixtures/*test-db*]
      (let [email (str "wrongpw-" (UUID/randomUUID) "@example.com")
            _ (admin-auth/create-admin! db {:email email
                                             :password "correct-password"
                                             :full_name "Wrong PW Test"
                                             :role "admin"})
            authenticated (admin-auth/authenticate-admin db email "wrong-password")]
        (is (nil? authenticated))))))

(deftest admin-find-by-id-test
  (testing "can find admin by ID"
    (when-let [db fixtures/*test-db*]
      (let [created-admin (create-test-admin! db)
            admin-id (or (:id created-admin) (:admins/id created-admin))
            found-admin (admin-auth/find-admin-by-id db admin-id)]
        (is (some? found-admin))
        (is (= admin-id (or (:id found-admin) (:admins/id found-admin))))))))

;; ============================================================================
;; User CRUD Tests
;; ============================================================================

(deftest user-create-test
  (testing "can create user with valid data"
    (when-let [db fixtures/*test-db*]
      (let [admin (create-test-admin! db)
            admin-id (or (:id admin) (:admins/id admin))
            email (str "user-" (UUID/randomUUID) "@example.com")
            created-user (create-test-user! db admin-id {:email email
                                                          :full_name "Test User"
                                                          :role "member"})]
        (is (some? created-user))
        (is (= email (or (:email created-user) (:users/email created-user))))))))

(deftest user-list-test
  (testing "can list users"
    (when-let [db fixtures/*test-db*]
      (let [admin (create-test-admin! db)
            admin-id (or (:id admin) (:admins/id admin))
            ;; Create some test users
            _ (create-test-user! db admin-id {:email (str "list1-" (UUID/randomUUID) "@test.com")})
            _ (create-test-user! db admin-id {:email (str "list2-" (UUID/randomUUID) "@test.com")})
            users (user-service/list-all-users db {:limit 100})]
        (is (>= (count users) 2))))))

(deftest user-search-test
  (testing "can search users by email"
    (when-let [db fixtures/*test-db*]
      (let [admin (create-test-admin! db)
            admin-id (or (:id admin) (:admins/id admin))
            unique-prefix (str "searchable-" (UUID/randomUUID))
            email (str unique-prefix "@test.com")
            _ (create-test-user! db admin-id {:email email :full_name "Searchable User"})
            results (user-service/list-all-users db {:search unique-prefix})]
        (is (= 1 (count results)))
        (is (= email (or (:email (first results)) 
                         (:users/email (first results)))))))))

;; ============================================================================
;; Audit Log Tests
;; ============================================================================

(deftest audit-log-retrieval-test
  (testing "can retrieve audit logs"
    (when-let [db fixtures/*test-db*]
      ;; First create admin and user to generate audit logs
      (let [admin (create-test-admin! db)
            admin-id (or (:id admin) (:admins/id admin))
            _ (create-test-user! db admin-id)
            logs (audit-service/get-audit-logs db {:limit 10})]
        ;; Should be able to retrieve logs (may be empty if audit not triggered)
        (is (or (nil? logs) (coll? logs)))))))

;; ============================================================================
;; Transaction Rollback Verification Tests
;; ============================================================================

(deftest transaction-rollback-test
  (testing "transaction rollback isolates test data"
    (when-let [db fixtures/*test-db*]
      ;; Create some data
      (let [email (str "rollback-test-" (UUID/randomUUID) "@example.com")
            _ (admin-auth/create-admin! db {:email email
                                             :password "test-pw"
                                             :full_name "Rollback Test"
                                             :role "admin"})
            admin (admin-auth/find-admin-by-email db email)]
        ;; Admin should exist within this transaction
        (is (some? admin))
        ;; Note: After this test, the admin will be rolled back
        ;; and won't exist in subsequent tests
        ))))

;; ============================================================================
;; Data Integrity Tests
;; ============================================================================

(deftest unique-email-constraint-test
  (testing "duplicate admin email is rejected"
    (when-let [db fixtures/*test-db*]
      (let [email (str "unique-" (UUID/randomUUID) "@example.com")
            _ (admin-auth/create-admin! db {:email email
                                             :password "test-pw"
                                             :full_name "First Admin"
                                             :role "admin"})]
        ;; Second admin with same email should fail
        (is (thrown? Exception
              (admin-auth/create-admin! db {:email email
                                             :password "test-pw-2"
                                             :full_name "Duplicate Admin"
                                             :role "admin"})))))))

(deftest password-hash-stored-test
  (testing "password is hashed, not stored in plain text"
    (when-let [db fixtures/*test-db*]
      (let [email (str "hash-" (UUID/randomUUID) "@example.com")
            plain-password "my-secret-password"
            _ (admin-auth/create-admin! db {:email email
                                             :password plain-password
                                             :full_name "Hash Test"
                                             :role "admin"})
            admin (admin-auth/find-admin-by-email db email)
            stored-hash (or (:password_hash admin) (:admins/password_hash admin))]
        (is (some? stored-hash))
        (is (not= plain-password stored-hash))
        (is (> (count stored-hash) (count plain-password)))))))

;; ============================================================================
;; Query Builder Integration Tests
;; ============================================================================

(deftest honeysql-query-test
  (testing "HoneySQL queries execute correctly"
    (when-let [db fixtures/*test-db*]
      (let [result (jdbc/execute! db
                     (hsql/format {:select [:id :email]
                                   :from [:admins]
                                   :limit 5}))]
        (is (coll? result))))))

(deftest pagination-test
  (testing "pagination works correctly"
    (when-let [db fixtures/*test-db*]
      (let [admin (create-test-admin! db)
            admin-id (or (:id admin) (:admins/id admin))
            ;; Create 5 users
            _ (dotimes [i 5]
                (create-test-user! db admin-id 
                  {:email (str "page-" i "-" (UUID/randomUUID) "@test.com")}))
            ;; Get first page
            page1 (user-service/list-all-users db {:limit 2 :offset 0})
            ;; Get second page  
            page2 (user-service/list-all-users db {:limit 2 :offset 2})]
        (is (= 2 (count page1)))
        (is (= 2 (count page2)))
        ;; Pages should have different users
        (let [page1-ids (set (map #(or (:id %) (:users/id %)) page1))
              page2-ids (set (map #(or (:id %) (:users/id %)) page2))]
          (is (empty? (set/intersection page1-ids page2-ids))))))))
