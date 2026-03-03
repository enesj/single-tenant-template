(ns app.e2e.multi-tenancy.auth-context-test
  "E2E tests for session & auth context (Manual §3).

   Allium rules: LoginSingleTenant, LoginMultiTenant, SelectTenant
   Allium guarantees: NoPasswordHashInResponse
   Covers: auth-status returns tenant info, no password hash, single-tenant auto-sets, multi-tenant shows selection."
  (:require
    [app.e2e.fixtures :as fixtures]
    [app.e2e.helpers :as h]
    [clojure.test :refer [deftest is testing use-fixtures]]))

(use-fixtures :each fixtures/with-browser-context)

;; ---------------------------------------------------------------------------
;; Test: Auth status returns tenant info after login
;; ---------------------------------------------------------------------------

(deftest auth-status-returns-tenant-info
  (testing "Auth status endpoint returns tenant and membership role"
    ;; Register + auto-login
    (h/api-register! h/user-a)
    (let [{:keys [status body]} (h/api-auth-status)]
      (is (= 200 status))
      (is (:authenticated body)
        "User should be authenticated")
      (is (some? (:tenant body))
        "Response should include tenant info")
      (is (some? (get-in body [:tenant :id]))
        "Tenant should have an ID")
      (is (some? (get-in body [:tenant :slug]))
        "Tenant should have a slug")
      (is (= "owner" (:membership-role body))
        "Membership role should be 'owner' for auto-provisioned user"))))

;; ---------------------------------------------------------------------------
;; Test: Password hash is never exposed in auth status
;; ---------------------------------------------------------------------------

(deftest no-password-hash-exposed
  (testing "Auth status never returns password_hash (security guarantee)"
    (h/api-register! h/user-a)
    (let [{:keys [body]} (h/api-auth-status)
          body-str (pr-str body)]
      (is (nil? (:password-hash body))
        "Top-level password-hash should be nil")
      (is (nil? (:password_hash body))
        "Top-level password_hash should be nil")
      (is (nil? (get-in body [:user :password-hash]))
        "User map should not contain password-hash")
      (is (nil? (get-in body [:user :password_hash]))
        "User map should not contain password_hash")
      (is (not (.contains body-str "password"))
        "No field with 'password' should appear anywhere in the response"))))

;; ---------------------------------------------------------------------------
;; Test: Single-tenant user auto-sets context (no selection page)
;; ---------------------------------------------------------------------------

(deftest single-tenant-auto-sets-context
  (testing "Single-tenant user is NOT redirected to /tenant-select"
    (h/api-register! h/user-a)
    (let [{:keys [body]} (h/api-auth-status)]
      (is (not (:tenant-selection-required body))
        "tenant-selection-required should be false for single-tenant user")
      (is (some? (:tenant body))
        "Tenant context should be auto-set"))))

;; ---------------------------------------------------------------------------
;; Test: Multi-tenant user sees selection page
;; ---------------------------------------------------------------------------

(deftest multi-tenant-shows-selection
  (testing "Multi-tenant user gets tenant-selection-required=true"
    ;; Create user-a with own tenant
    (h/api-register! h/user-a)

    ;; Create user-b with own tenant, then invite user-a to user-b's tenant
    (let [ctx-b (.newContext (fixtures/get-browser))]
      (try
        ;; Register user-b in a separate context
        (h/api-register! ctx-b h/user-b)
        ;; Invite user-a to user-b's tenant
        (h/api-invite-member! ctx-b {:email (:email h/user-a) :role "member"})
        (finally
          (.close ctx-b))))

    ;; Accept the invitation as user-a
    (let [token (h/get-invitation-token (:email h/user-a))]
      (is (some? token) "Invitation token should exist in DB")
      (h/api-accept-invitation! token))

    ;; Logout and login again — should now require tenant selection
    (h/api-logout!)
    (let [{:keys [body]} (h/api-login! h/user-a)]
      (is (:tenant-selection-required body)
        "tenant-selection-required should be true for multi-tenant user")
      (is (some? (:available-tenants body))
        "available-tenants should be present")
      (is (>= (count (:available-tenants body)) 2)
        "Should have at least 2 available tenants"))))
