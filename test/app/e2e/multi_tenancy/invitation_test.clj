(ns app.e2e.multi-tenancy.invitation-test
  "E2E tests for invitation flow (Manual §5).

   Allium rules: InviteMember, AcceptInvitation, RevokeInvitation
   Covers: send invitation, duplicate blocked, accept creates membership, revoke invalidates token."
  (:require
    [app.e2e.fixtures :as fixtures]
    [app.e2e.helpers :as h]
    [clojure.test :refer [deftest is testing use-fixtures]]))

(use-fixtures :each fixtures/with-browser-context)

;; ---------------------------------------------------------------------------
;; Test: Sending an invitation appears in pending list
;; ---------------------------------------------------------------------------

(deftest send-invitation-appears-in-list
  (testing "Owner can send an invitation that appears as pending"
    ;; Register user-a (tenant owner)
    (h/api-register! h/user-a)

    ;; Invite user-c to the tenant
    (let [{:keys [status body]} (h/api-invite-member! {:email (:email h/user-c) :role "member"})]
      (is (= 200 status) "Invitation should succeed")
      (is (get-in body [:success])
        "Response should indicate success"))

    ;; Verify invitation exists in DB
    (let [inv (h/get-invitation-by-email (:email h/user-c))]
      (is (some? inv) "Invitation should exist in DB")
      (is (= "pending" (:status inv))
        "Invitation should be in 'pending' status")
      (is (= "member" (:role inv))
        "Invitation role should be 'member'")
      (is (some? (:token inv))
        "Invitation should have a token"))))

;; ---------------------------------------------------------------------------
;; Test: Duplicate invitation is blocked
;; ---------------------------------------------------------------------------

(deftest duplicate-invitation-blocked
  (testing "Cannot send a duplicate invitation to the same email"
    (h/api-register! h/user-a)

    ;; First invitation succeeds
    (let [{:keys [status]} (h/api-invite-member! {:email (:email h/user-c) :role "member"})]
      (is (= 200 status)))

    ;; Second invitation to same email should fail
    (let [{:keys [status body]} (h/api-invite-member! {:email (:email h/user-c) :role "admin"})]
      (is (not= 200 status)
        "Duplicate invitation should be rejected")
      (is (or (= 409 status) (= 422 status) (= 400 status))
        "Should return a conflict/validation error status"))))

;; ---------------------------------------------------------------------------
;; Test: Accepting invitation creates a membership
;; ---------------------------------------------------------------------------

(deftest accept-invitation-creates-membership
  (testing "Accepting an invitation creates an active membership"
    ;; Register user-a (owner) and invite user-c
    (h/api-register! h/user-a)
    (h/api-invite-member! {:email (:email h/user-c) :role "member"})

    ;; Register user-c in a separate context so they exist
    (let [ctx-c (.newContext (fixtures/get-browser))]
      (try
        (h/api-register! ctx-c h/user-c)

        ;; Accept the invitation as user-c
        (let [token (h/get-invitation-token (:email h/user-c))]
          (is (some? token) "Invitation token should exist")
          (let [{:keys [status]} (h/api-accept-invitation! ctx-c token)]
            (is (= 200 status) "Accepting invitation should succeed")))

        ;; Verify membership in DB
        (let [memberships (h/get-memberships-for-user (:email h/user-c))]
          (is (= 2 (count memberships))
            "User C should now have 2 memberships (own tenant + invited)")
          (let [invited-membership (first (filter #(= "e2e-user-a" (:tenant_slug %)) memberships))]
            (is (some? invited-membership)
              "Should have a membership in User A's tenant")
            (is (= "member" (:role invited-membership))
              "Invited membership should have 'member' role")
            (is (= "active" (:status invited-membership))
              "Invited membership should be 'active'")))

        ;; Verify invitation status changed to accepted
        (let [inv (h/get-invitation-by-email (:email h/user-c))]
          (is (= "accepted" (:status inv))
            "Invitation status should be 'accepted'"))

        (finally
          (.close ctx-c))))))

;; ---------------------------------------------------------------------------
;; Test: Revoking invitation invalidates the token
;; ---------------------------------------------------------------------------

(deftest revoke-invitation-invalidates-token
  (testing "Revoking an invitation prevents acceptance"
    ;; Register user-a and invite user-d
    (h/api-register! h/user-a)
    (h/api-invite-member! {:email (:email h/user-d) :role "viewer"})

    ;; Get the invitation ID and revoke it
    (let [inv (h/get-invitation-by-email (:email h/user-d))
          inv-id (:id inv)
          token (:token inv)]
      (is (some? inv-id) "Invitation should exist")

      ;; Revoke the invitation
      (let [{:keys [status]} (h/api-revoke-invitation! inv-id)]
        (is (= 200 status) "Revoking should succeed"))

      ;; Verify invitation is revoked in DB
      (let [updated-inv (h/get-invitation-by-email (:email h/user-d))]
        (is (= "revoked" (:status updated-inv))
          "Invitation should be revoked"))

      ;; Try to accept the revoked invitation in a new context
      (let [ctx-d (.newContext (fixtures/get-browser))]
        (try
          (h/api-register! ctx-d h/user-d)
          (let [{:keys [status]} (h/api-accept-invitation! ctx-d token)]
            (is (not= 200 status)
              "Accepting a revoked invitation should fail"))
          (finally
            (.close ctx-d)))))))
