(ns app.backend.routes.admin.user-operations-test
  "Tests for admin advanced user operations routes.
   
   Tests cover:
   - Update user role
   - Force verify email
   - Reset user password
   - Get user activity
   - Impersonate user
   - Advanced user search"
  (:require
    [app.backend.routes.admin.user-operations :as user-ops]
    [app.backend.services.admin :as admin-service]
    [app.backend.test-helpers :as h]
    [app.shared.field-metadata :as field-meta]
    [clojure.test :refer [deftest is testing use-fixtures]]))

;; ============================================================================
;; Test Fixtures
;; ============================================================================

(use-fixtures :each h/with-clean-test-state)

;; ============================================================================
;; Test Data
;; ============================================================================

(def test-admin-id (java.util.UUID/randomUUID))
(def test-user-id (java.util.UUID/randomUUID))

(def mock-admin
  {:id test-admin-id
   :email "admin@example.com"
   :full_name "Test Admin"
   :role "owner"})

(def mock-models
  {:users {:fields [{:name :role
                     :type :enum
                     :choices ["user" "premium" "admin"]}]}})

;; ============================================================================
;; Handler Creation Tests
;; ============================================================================

(deftest handler-creation-test
  (testing "update-user-role-handler returns a function"
    (let [db (h/mock-db)
          handler (user-ops/update-user-role-handler db mock-models)]
      (is (fn? handler))))
  
  (testing "force-verify-email-handler returns a function"
    (let [db (h/mock-db)
          handler (user-ops/force-verify-email-handler db)]
      (is (fn? handler))))
  
  (testing "reset-user-password-handler returns a function"
    (let [db (h/mock-db)
          handler (user-ops/reset-user-password-handler db)]
      (is (fn? handler))))
  
  (testing "get-user-activity-handler returns a function"
    (let [db (h/mock-db)
          handler (user-ops/get-user-activity-handler db)]
      (is (fn? handler))))
  
  (testing "impersonate-user-handler returns a function"
    (let [db (h/mock-db)
          handler (user-ops/impersonate-user-handler db)]
      (is (fn? handler))))
  
  (testing "advanced-user-search-handler returns a function"
    (let [db (h/mock-db)
          handler (user-ops/advanced-user-search-handler db)]
      (is (fn? handler)))))

;; ============================================================================
;; Update User Role Tests
;; ============================================================================

(deftest update-user-role-handler-test
  (testing "update-user-role updates role successfully"
    (let [db (h/mock-db)
          handler (user-ops/update-user-role-handler db mock-models)
          request (h/mock-admin-request :put (str "/admin/api/users/role/" test-user-id) mock-admin
                    {:path-params {:id (str test-user-id)}
                     :body {:role "premium"}})]
      (with-redefs [field-meta/get-enum-choices
                    (constantly ["user" "premium" "admin"])
                    admin-service/update-user-role!
                    (fn [_db user-id role _admin-id _ip _ua]
                      (is (= test-user-id user-id))
                      (is (= "premium" role))
                      {:success true})]
        (let [response (handler request)]
          (is (= 200 (:status response)))))))
  
  (testing "update-user-role returns error for invalid role"
    (let [db (h/mock-db)
          handler (user-ops/update-user-role-handler db mock-models)
          request (h/mock-admin-request :put (str "/admin/api/users/role/" test-user-id) mock-admin
                    {:path-params {:id (str test-user-id)}
                     :body {:role "invalid-role"}})]
      (with-redefs [field-meta/get-enum-choices
                    (constantly ["user" "premium" "admin"])]
        (let [response (handler request)]
          (is (= 400 (:status response)))))))
  
  (testing "update-user-role returns error when role missing"
    (let [db (h/mock-db)
          handler (user-ops/update-user-role-handler db mock-models)
          request (h/mock-admin-request :put (str "/admin/api/users/role/" test-user-id) mock-admin
                    {:path-params {:id (str test-user-id)}
                     :body {}})]
      (with-redefs [field-meta/get-enum-choices
                    (constantly ["user" "premium" "admin"])]
        (let [response (handler request)]
          (is (= 400 (:status response))))))))

;; ============================================================================
;; Force Verify Email Tests
;; ============================================================================

(deftest force-verify-email-handler-test
  (testing "force-verify-email verifies user email"
    (let [db (h/mock-db)
          handler (user-ops/force-verify-email-handler db)
          request (h/mock-admin-request :post (str "/admin/api/users/verify-email/" test-user-id) mock-admin
                    {:path-params {:id (str test-user-id)}})]
      (with-redefs [admin-service/force-verify-email!
                    (fn [_db user-id _admin-id _ip _ua]
                      (is (= test-user-id user-id))
                      {:success true})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (:success body)))))))

;; ============================================================================
;; Reset User Password Tests
;; ============================================================================

(deftest reset-user-password-handler-test
  (testing "reset-user-password resets password successfully"
    (let [db (h/mock-db)
          handler (user-ops/reset-user-password-handler db)
          request (h/mock-admin-request :post (str "/admin/api/users/reset-password/" test-user-id) mock-admin
                    {:path-params {:id (str test-user-id)}})]
      (with-redefs [admin-service/reset-user-password!
                    (fn [_db user-id _admin-id _ip _ua]
                      (is (= test-user-id user-id))
                      {:success true :temporary-password "TempPass123!"})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (:success body))))))
  
  (testing "reset-user-password handles failure"
    (let [db (h/mock-db)
          handler (user-ops/reset-user-password-handler db)
          request (h/mock-admin-request :post (str "/admin/api/users/reset-password/" test-user-id) mock-admin
                    {:path-params {:id (str test-user-id)}})]
      (with-redefs [admin-service/reset-user-password!
                    (fn [_db _user-id _admin-id _ip _ua]
                      {:success false :message "User not found"})]
        (let [response (handler request)]
          (is (= 400 (:status response))))))))

;; ============================================================================
;; Get User Activity Tests
;; ============================================================================

(deftest get-user-activity-handler-test
  (testing "get-user-activity returns activity data"
    (let [db (h/mock-db)
          handler (user-ops/get-user-activity-handler db)
          request (h/mock-admin-request :get (str "/admin/api/users/activity/" test-user-id) mock-admin
                    {:path-params {:id (str test-user-id)}})]
      (with-redefs [admin-service/get-user-activity
                    (fn [_db user-id _pagination]
                      (is (= test-user-id user-id))
                      [{:action "login" :timestamp "2025-01-01T00:00:00Z"}])]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (vector? (:activity body))))))))

;; ============================================================================
;; Advanced User Search Tests
;; ============================================================================

(deftest advanced-user-search-handler-test
  (testing "advanced-user-search returns filtered users"
    (let [db (h/mock-db)
          handler (user-ops/advanced-user-search-handler db)
          request (h/mock-admin-request :get "/admin/api/users/search" mock-admin
                    {:params {:search "test" :status "active" :role "user"}})]
      (with-redefs [admin-service/search-users-advanced
                    (fn [_db filters]
                      (is (= "test" (:search filters)))
                      (is (= "active" (:status filters)))
                      [{:id test-user-id :email "test@example.com"}])]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (vector? (:users body)))))))
  
  (testing "advanced-user-search handles pagination"
    (let [db (h/mock-db)
          handler (user-ops/advanced-user-search-handler db)
          request (h/mock-admin-request :get "/admin/api/users/search" mock-admin
                    {:params {:limit "10" :offset "20"}})]
      (with-redefs [admin-service/search-users-advanced
                    (fn [_db filters]
                      (is (= 10 (:limit filters)))
                      (is (= 20 (:offset filters)))
                      [])]
        (let [response (handler request)]
          (is (= 200 (:status response))))))))

;; ============================================================================
;; Route Definition Tests
;; ============================================================================

(deftest routes-test
  (testing "routes function returns route definitions"
    (let [db (h/mock-db)
          service-container {:models-data mock-models}
          routes (user-ops/routes db service-container)]
      (is (vector? routes))
      (is (= "" (first routes))))))
