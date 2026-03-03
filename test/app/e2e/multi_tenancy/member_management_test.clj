(ns app.e2e.multi-tenancy.member-management-test
  "E2E tests for member management (Manual §6).

   Allium rules: ChangeMemberRole, RemoveMember, TransferOwnership
   Allium guarantees: OwnerProtection, SingleOwner
   Covers: change role, remove member, transfer ownership, cannot remove owner, cannot invite as owner."
  (:require
    [app.e2e.fixtures :as fixtures]
    [app.e2e.helpers :as h]
    [clojure.test :refer [deftest is testing use-fixtures]]))

(use-fixtures :each fixtures/with-browser-context)

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- setup-owner-with-member!
  "Register user-a as owner, invite and accept user-c as member.
   Returns {:owner-ctx <this context>, :member-membership <map>}."
  []
  ;; Register user-a (owner)
  (h/api-register! h/user-a)

  ;; Invite user-c
  (h/api-invite-member! {:email (:email h/user-c) :role "member"})

  ;; Register user-c in separate context and accept
  (let [ctx-c (.newContext (fixtures/get-browser))]
    (try
      (h/api-register! ctx-c h/user-c)
      (let [token (h/get-invitation-token (:email h/user-c))]
        (h/api-accept-invitation! ctx-c token))
      (finally
        (.close ctx-c))))

  ;; Find user-c's membership in user-a's tenant
  (let [memberships (h/get-memberships-for-user (:email h/user-c))
        member-m (first (filter #(= "e2e-user-a" (:tenant_slug %)) memberships))]
    {:member-membership member-m}))

;; ---------------------------------------------------------------------------
;; Test: Change member role
;; ---------------------------------------------------------------------------

(deftest change-member-role
  (testing "Owner can change a member's role"
    (let [{:keys [member-membership]} (setup-owner-with-member!)
          m-id (:id member-membership)]
      (is (= "member" (:role member-membership))
        "Should start as 'member'")

      ;; Change to admin
      (let [{:keys [status]} (h/api-change-role! m-id "admin")]
        (is (= 200 status) "Role change should succeed"))

      ;; Verify in DB
      (let [memberships (h/get-memberships-for-user (:email h/user-c))
            updated (first (filter #(= "e2e-user-a" (:tenant_slug %)) memberships))]
        (is (= "admin" (:role updated))
          "Membership role should now be 'admin'")))))

;; ---------------------------------------------------------------------------
;; Test: Remove member
;; ---------------------------------------------------------------------------

(deftest remove-member
  (testing "Owner can remove a member from the tenant"
    (let [{:keys [member-membership]} (setup-owner-with-member!)
          m-id (:id member-membership)]

      ;; Remove user-c
      (let [{:keys [status]} (h/api-remove-member! m-id)]
        (is (= 200 status) "Member removal should succeed"))

      ;; Verify membership is suspended in DB
      (let [all-memberships (h/query-db
                              "SELECT tm.*, t.slug as tenant_slug
                               FROM tenant_memberships tm
                               JOIN users u ON u.id = tm.user_id
                               JOIN tenants t ON t.id = tm.tenant_id
                               WHERE u.email = ? AND t.slug = 'e2e-user-a'"
                              (:email h/user-c))
            m (first all-memberships)]
        (is (= "suspended" (:status m))
          "Removed member's status should be 'suspended'")))))

;; ---------------------------------------------------------------------------
;; Test: Transfer ownership
;; ---------------------------------------------------------------------------

(deftest transfer-ownership
  (testing "Owner can transfer ownership to another member"
    (let [{:keys [member-membership]} (setup-owner-with-member!)]

      ;; First promote to admin (transfer requires admin or member)
      (h/api-change-role! (:id member-membership) "admin")

      ;; Get user-c's user ID
      (let [user-c-db (h/get-user-by-email (:email h/user-c))]

        ;; Transfer ownership
        (let [{:keys [status]} (h/api-transfer-ownership! (:id user-c-db))]
          (is (= 200 status) "Ownership transfer should succeed"))

        ;; Verify: user-c is now owner, user-a is now admin
        (let [memberships-a (h/get-memberships-for-tenant "e2e-user-a")
              user-a-m (first (filter #(= (:email h/user-a) (:email %)) memberships-a))
              user-c-m (first (filter #(= (:email h/user-c) (:email %)) memberships-a))]
          (is (= "admin" (:role user-a-m))
            "Original owner should now be 'admin'")
          (is (= "owner" (:role user-c-m))
            "Target user should now be 'owner'"))))))

;; ---------------------------------------------------------------------------
;; Test: Cannot remove owner
;; ---------------------------------------------------------------------------

(deftest cannot-remove-owner
  (testing "Owner cannot be removed from the tenant"
    (setup-owner-with-member!)

    ;; Find owner's membership
    (let [memberships (h/get-memberships-for-tenant "e2e-user-a")
          owner-m (first (filter #(= "owner" (:role %)) memberships))]
      (is (some? owner-m) "Should have an owner membership")

      ;; Try to remove the owner — should fail
      (let [{:keys [status]} (h/api-remove-member! (:id owner-m))]
        (is (not= 200 status)
          "Removing the owner should fail")
        (is (or (= 403 status) (= 400 status) (= 422 status))
          "Should return a forbidden or validation error")))))

;; ---------------------------------------------------------------------------
;; Test: Cannot invite as owner role
;; ---------------------------------------------------------------------------

(deftest cannot-invite-as-owner
  (testing "Cannot send an invitation with 'owner' role"
    (h/api-register! h/user-a)

    (let [{:keys [status]} (h/api-invite-member! {:email "nobody@example.com" :role "owner"})]
      (is (not= 200 status)
        "Inviting as 'owner' should be rejected")
      (is (or (= 400 status) (= 422 status) (= 403 status))
        "Should return a validation or forbidden error"))))
