(ns app.backend.routes.admin.user-operations-test
  "Tests for admin advanced user operations routes.

   Tests cover:
   - Force verify email
   - Reset user password
   - Get user activity
   - Impersonate user
   - Advanced user search"
  (:require
    [app.template.backend.routes.admin.user-operations :as user-ops]
    [app.admin.backend.services.admin.users :as admin-users]
    [app.admin.backend.services.admin.users.bulk :as admin-users-bulk]
    [app.admin.backend.services.admin.users.security :as user-security]
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
(def test-user-id (java.util.UUID/randomUUID))

(def mock-admin
  {:id test-admin-id
   :email "admin@example.com"
   :full_name "Test Admin"
   :role "owner"})

;; ============================================================================
;; Handler Creation Tests
;; ============================================================================

(deftest handler-creation-test
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
;; Force Verify Email Tests
;; ============================================================================

(deftest force-verify-email-handler-test
  (testing "force-verify-email verifies user email"
    (let [db (h/mock-db)
          handler (user-ops/force-verify-email-handler db)
          request (h/mock-admin-request :post (str "/admin/api/users/verify-email/" test-user-id) mock-admin
                    {:path-params {:id (str test-user-id)}})]
      (with-redefs [user-security/force-verify-email!
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
      (with-redefs [user-security/reset-user-password!
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
      (with-redefs [user-security/reset-user-password!
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
      (with-redefs [admin-users/get-user-activity
                    (fn [_db user-id _pagination]
                      (is (= test-user-id user-id))
                      [{:action "login" :timestamp "2025-01-01T00:00:00Z"}])]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (vector? (:activity body))))))))

;; ============================================================================
;; Impersonate User Tests
;; ============================================================================

(deftest impersonate-user-handler-test
  (testing "impersonate-user sets user auth-session and preserves existing session keys"
    (let [db (h/mock-db)
          handler (user-ops/impersonate-user-handler db)
          request (-> (h/mock-admin-request :post (str "/admin/api/users/impersonate/" test-user-id) mock-admin
                        {:path-params {:id (str test-user-id)}})
                    (assoc :session {:admin-token "existing-admin-token"
                                     :other "keep"}))
          auth-session {:user {:id (str test-user-id)
                               :email "user@example.com"}
                        :provider "impersonation"}]
      (with-redefs [admin-users-bulk/create-user-impersonation-session!
                    (fn [_db user-id admin-id _ip _ua]
                      (is (= test-user-id user-id))
                      (is (= test-admin-id admin-id))
                      {:success true
                       :redirect-url "/dashboard"
                       :auth-session auth-session})]
        (let [response (handler request)
              body (h/parse-response-body response)]
          (is (= 200 (:status response)))
          (is (= "/dashboard" (:redirect-url body)))
          (is (= auth-session (get-in response [:session :auth-session])))
          (is (= "existing-admin-token" (get-in response [:session :admin-token])))
          (is (= "keep" (get-in response [:session :other])))))))

  (testing "impersonate-user returns 400 and does not set auth-session on failure"
    (let [db (h/mock-db)
          handler (user-ops/impersonate-user-handler db)
          request (-> (h/mock-admin-request :post (str "/admin/api/users/impersonate/" test-user-id) mock-admin
                        {:path-params {:id (str test-user-id)}})
                    (assoc :session {:admin-token "existing-admin-token"}))]
      (with-redefs [admin-users-bulk/create-user-impersonation-session!
                    (fn [_db _user-id _admin-id _ip _ua]
                      {:error "nope"})]
        (let [response (handler request)]
          (is (= 400 (:status response)))
          ;; On failure we do not modify the session, so the response will not
          ;; include a :session key at all.
          (is (nil? (:session response))))))))

;; ============================================================================
;; Advanced User Search Tests
;; ============================================================================

(deftest advanced-user-search-handler-test
  (testing "advanced-user-search returns filtered users"
    (let [db (h/mock-db)
          handler (user-ops/advanced-user-search-handler db)
          request (h/mock-admin-request :get "/admin/api/users/search" mock-admin
                    {:params {:search "test" :status "active" :role "user"}})]
      (with-redefs [admin-users/search-users-advanced
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
      (with-redefs [admin-users/search-users-advanced
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
