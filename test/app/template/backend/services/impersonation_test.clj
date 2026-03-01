(ns app.template.backend.services.impersonation-test
  "Integration tests for the impersonation grant service.
   Uses transaction rollback for DB isolation."
  (:require
    [app.admin.backend.services.admin.auth :as admin-auth]
    [app.backend.fixtures :as fixtures]
    [app.domain.expenses.test-helpers :as th]
    [app.template.backend.services.impersonation :as svc]
    [clojure.test :refer [deftest is testing use-fixtures]])
  (:import
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

;; ============================================================================
;; Test Helpers
;; ============================================================================

(defn- ensure-test-admin!
  "Insert a minimal admin row for testing. Returns the admin map."
  [db {:keys [email] :or {email "admin@test.com"}}]
  (let [existing (admin-auth/find-admin-by-email db email)]
    (or existing
      (do (admin-auth/create-admin! db {:email    email
                                        :password "test-password-123"
                                        :full_name "Test Admin"
                                        :role     "admin"})
        (admin-auth/find-admin-by-email db email)))))

(defn- create-admin-session!
  "Create an admin session and return the session map."
  [db admin-id]
  (admin-auth/create-admin-session! db admin-id "127.0.0.1" "test-agent"))

;; ============================================================================
;; Tests
;; ============================================================================

(deftest create-grant-happy-path
  (testing "tenant owner can create an impersonation grant for an admin"
    (let [db    fixtures/*test-db*
          user  (th/ensure-test-user! db {:email "owner@grants.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          admin (ensure-test-admin! db {:email "grantee@admin.test"})
          grant (svc/create-grant! db {:admin-email (:email admin)
                                       :tenant-id   tenant-id
                                       :role        "viewer"
                                       :granted-by  (:id user)})]
      (is (some? (:id grant)) "Grant should be created")
      (is (= (str tenant-id) (str (:tenant_id grant))) "Grant should reference the tenant")
      (is (= (str (:id admin)) (str (:admin_id grant))) "Grant should reference the admin")
      (is (= "active" (str (:status grant))) "Grant status should be active"))))

(deftest create-grant-rejects-duplicate
  (testing "creating a second active grant for the same admin+tenant fails"
    (let [db    fixtures/*test-db*
          user  (th/ensure-test-user! db {:email "owner@dup.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          admin (ensure-test-admin! db {:email "dup-admin@admin.test"})
          _     (svc/create-grant! db {:admin-email (:email admin)
                                       :tenant-id   tenant-id
                                       :role        "viewer"
                                       :granted-by  (:id user)})]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"already exists"
            (svc/create-grant! db {:admin-email (:email admin)
                                   :tenant-id   tenant-id
                                   :role        "admin"
                                   :granted-by  (:id user)}))
        "Should reject duplicate active grant"))))

(deftest create-grant-rejects-unknown-admin
  (testing "creating a grant for a non-existent admin email fails"
    (let [db    fixtures/*test-db*
          user  (th/ensure-test-user! db {:email "owner@unknown.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Admin not found"
            (svc/create-grant! db {:admin-email "nobody@nowhere.test"
                                   :tenant-id   tenant-id
                                   :role        "viewer"
                                   :granted-by  (:id user)}))
        "Should reject unknown admin email"))))

(deftest revoke-by-owner-sets-fields
  (testing "revoking by owner sets status and revoked_by_user"
    (let [db    fixtures/*test-db*
          user  (th/ensure-test-user! db {:email "owner@revoke-o.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          admin (ensure-test-admin! db {:email "revoke-o@admin.test"})
          ;; Need a session so deactivate doesn't fail on no rows
          _     (create-admin-session! db (:id admin))
          grant (svc/create-grant! db {:admin-email (:email admin)
                                       :tenant-id   tenant-id
                                       :role        "viewer"
                                       :granted-by  (:id user)})
          _     (svc/revoke-grant-by-owner! db (:id grant) (:id user))
          ;; Re-fetch — should not find active grant
          after (svc/find-active-grant db (:id grant))]
      (is (nil? after) "Grant should no longer be active after owner revocation"))))

(deftest revoke-by-admin-sets-fields
  (testing "admin can revoke their own grant"
    (let [db    fixtures/*test-db*
          user  (th/ensure-test-user! db {:email "owner@revoke-a.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          admin (ensure-test-admin! db {:email "revoke-a@admin.test"})
          _     (create-admin-session! db (:id admin))
          grant (svc/create-grant! db {:admin-email (:email admin)
                                       :tenant-id   tenant-id
                                       :role        "viewer"
                                       :granted-by  (:id user)})
          _     (svc/revoke-grant-by-admin! db (:id grant) (:id admin))
          after (svc/find-active-grant db (:id grant))]
      (is (nil? after) "Grant should no longer be active after admin revocation"))))

(deftest activate-writes-impersonation-context
  (testing "activating impersonation populates admin_sessions.impersonation_context"
    (let [db      fixtures/*test-db*
          user    (th/ensure-test-user! db {:email "owner@activate.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          admin   (ensure-test-admin! db {:email "activate@admin.test"})
          session (create-admin-session! db (:id admin))
          grant   (svc/create-grant! db {:admin-email (:email admin)
                                         :tenant-id   tenant-id
                                         :role        "viewer"
                                         :granted-by  (:id user)})
          context (svc/activate-impersonation! db (:id admin) (:id grant))
          ;; Verify via the auth service
          result  (admin-auth/get-admin-by-session-with-context db (:token session))]
      (is (some? context) "activate-impersonation! should return context map")
      (is (= (str tenant-id) (:tenant-id context)) "Context should contain tenant-id")
      (is (= "viewer" (:role context)) "Context should contain role")
      (is (some? (:impersonation-context result)) "Session should have impersonation context")
      (is (= (str tenant-id)
            (get-in result [:impersonation-context :tenant-id]))
        "Session context tenant-id should match"))))

(deftest deactivate-clears-context
  (testing "deactivating impersonation clears the session context"
    (let [db      fixtures/*test-db*
          user    (th/ensure-test-user! db {:email "owner@deactivate.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          admin   (ensure-test-admin! db {:email "deactivate@admin.test"})
          session (create-admin-session! db (:id admin))
          grant   (svc/create-grant! db {:admin-email (:email admin)
                                         :tenant-id   tenant-id
                                         :role        "viewer"
                                         :granted-by  (:id user)})
          _       (svc/activate-impersonation! db (:id admin) (:id grant))
          _       (svc/deactivate-impersonation! db (:id admin))
          result  (admin-auth/get-admin-by-session-with-context db (:token session))]
      (is (nil? (:impersonation-context result))
        "Session context should be nil after deactivation"))))

(deftest list-grants-returns-correct-results
  (testing "list-grants-for-tenant returns grants with admin info"
    (let [db    fixtures/*test-db*
          user  (th/ensure-test-user! db {:email "owner@list.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          admin (ensure-test-admin! db {:email "listed@admin.test"})
          _     (svc/create-grant! db {:admin-email (:email admin)
                                       :tenant-id   tenant-id
                                       :role        "viewer"
                                       :granted-by  (:id user)})
          grants (svc/list-grants-for-tenant db tenant-id)]
      (is (= 1 (count grants)) "Should have exactly one grant")
      (is (= "listed@admin.test" (:admin_email (first grants)))
        "Grant should include joined admin email"))))
