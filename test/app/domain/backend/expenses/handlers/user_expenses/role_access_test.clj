(ns app.domain.backend.expenses.handlers.user-expenses.role-access-test
  "Unit tests for role-based access control — pure functions, no DB required.

  Verifies that `get-user-role` reads the membership role first, and that
  the role-set constants enforce the correct deny rules."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [clojure.test :refer [deftest is testing]])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Test helpers
;; ============================================================================

(defn- mock-request
  "Build a minimal Ring request map with both membership and user roles set.

  The global user role defaults to \"member\" (password users), but the
  membership role is what the tenant system should check."
  ([membership-role]
   (mock-request membership-role "member"))
  ([membership-role user-role]
   {:session {:auth-session {:user       {:id   (UUID/randomUUID)
                                          :role user-role}
                             :membership {:id   (UUID/randomUUID)
                                          :role membership-role}
                             :tenant     {:id (UUID/randomUUID)}}}}))

(defn- mock-request-no-membership
  "Request with only a global user role (pre-Phase 1 sessions / no tenant)."
  [user-role]
  {:session {:auth-session {:user {:id   (UUID/randomUUID)
                                   :role user-role}}}})

;; ============================================================================
;; get-user-role — membership role takes priority
;; ============================================================================

(deftest get-user-role-returns-membership-role
  (testing "membership role is used over global user role"
    (is (= "viewer" (h/get-user-role (mock-request "viewer" "member"))))
    (is (= "admin"  (h/get-user-role (mock-request "admin" "member"))))
    (is (= "owner"  (h/get-user-role (mock-request "owner" "member"))))))

(deftest get-user-role-falls-back-to-user-role
  (testing "when no membership role is present, falls back to global user role"
    (is (= "member" (h/get-user-role (mock-request-no-membership "member"))))
    (is (= "admin"  (h/get-user-role (mock-request-no-membership "admin"))))))

(deftest get-user-role-handles-keyword-roles
  (testing "keyword roles are normalized to strings"
    (is (= "viewer" (h/get-user-role
                      {:session {:auth-session {:membership {:role :viewer}}}})))))

(deftest get-user-role-returns-nil-when-missing
  (testing "returns nil for empty request"
    (is (nil? (h/get-user-role {})))
    (is (nil? (h/get-user-role {:session {}})))))

;; ============================================================================
;; tenant-elevated? — owner/admin membership check
;; ============================================================================

(deftest tenant-elevated-true-for-admin-and-owner
  (is (true? (h/tenant-elevated? (mock-request "admin"))))
  (is (true? (h/tenant-elevated? (mock-request "owner")))))

(deftest tenant-elevated-false-for-member-and-viewer
  (is (false? (h/tenant-elevated? (mock-request "member"))))
  (is (false? (h/tenant-elevated? (mock-request "viewer")))))

(deftest tenant-elevated-false-when-no-role
  (is (false? (h/tenant-elevated? {}))))

;; ============================================================================
;; AllowExpenseRead — all 4 roles can read
;; ============================================================================

(deftest allow-expense-read-all-roles
  (testing "all membership roles pass expenses-read-roles"
    (doseq [role ["viewer" "member" "admin" "owner"]]
      (is (nil? (h/ensure-role (mock-request role) h/expenses-read-roles "test"))
        (str role " should be allowed to read expenses")))))

;; ============================================================================
;; AllowExpenseWrite — member/admin/owner pass, viewer blocked
;; ============================================================================

(deftest allow-expense-write-member-admin-owner
  (doseq [role ["member" "admin" "owner"]]
    (is (nil? (h/ensure-role (mock-request role) h/expenses-write-roles "test"))
      (str role " should be allowed to write expenses"))))

;; ============================================================================
;; DenyViewerWrites — viewer blocked on all write role sets
;; ============================================================================

(deftest deny-viewer-writes
  (testing "viewer is blocked from all write operations"
    (let [request (mock-request "viewer")]
      (is (some? (h/ensure-role request h/expenses-write-roles "test"))
        "viewer cannot write expenses")
      (is (some? (h/ensure-role request h/receipts-write-roles "test"))
        "viewer cannot write receipts")
      (is (some? (h/ensure-role request h/reference-data-write-roles "test"))
        "viewer cannot write reference data"))))

(deftest deny-viewer-writes-returns-403
  (testing "viewer gets a 403 response, not just truthy"
    (let [response (h/ensure-role (mock-request "viewer") h/expenses-write-roles "Forbidden")]
      (is (= 403 (:status response))))))

;; ============================================================================
;; DenyMemberTenantAdmin — member blocked on admin/owner-only sets
;; ============================================================================

(deftest deny-member-tenant-admin
  (testing "member cannot access admin/owner-only operations"
    (let [request (mock-request "member")
          admin-owner-roles #{"admin" "owner"}]
      (is (some? (h/ensure-role request admin-owner-roles "test"))
        "member should be blocked from admin/owner operations"))))

(deftest allow-member-read-reference-data
  (testing "member can read reference data"
    (is (nil? (h/ensure-role (mock-request "member") h/reference-data-read-roles "test")))))

;; ============================================================================
;; Fallback: no membership role → uses global user role
;; ============================================================================

(deftest fallback-user-role-when-no-membership
  (testing "without membership, global user role is used for role checks"
    (let [request (mock-request-no-membership "member")]
      (is (nil? (h/ensure-role request h/expenses-write-roles "test"))
        "member user-role should pass write check")
      (is (nil? (h/ensure-role request h/expenses-read-roles "test"))
        "member user-role should pass read check"))))

(deftest fallback-admin-user-role-passes-admin-checks
  (testing "admin user-role (OAuth) passes admin-only checks when no membership"
    (let [request (mock-request-no-membership "admin")]
      (is (nil? (h/ensure-role request #{"admin" "owner"} "test"))))))
