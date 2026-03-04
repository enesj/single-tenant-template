(ns app.template.backend.services.member-test
  "Tests for role changes, ownership transfer, and member removal guards."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.template.backend.services.invitation :as invitation-svc]
    [app.template.backend.services.member :as member-svc]
    [app.template.backend.services.tenant :as tenant-svc]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]))

(use-fixtures :each fixtures/with-transaction-rollback)

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- create-user! [db suffix]
  (let [id (java.util.UUID/randomUUID)
        now (java.time.LocalDateTime/now)]
    (jdbc/execute-one! db
      (sql/format {:insert-into [:users]
                   :values [{:id id
                             :email (str "member-test-" suffix "-" id "@example.com")
                             :full_name (str "User " suffix)
                             :password_hash "placeholder"

                             :status [:cast "active" :user_status]
                             :auth_provider "password"
                             :email_verified false
                             :created_at now :updated_at now}]
                   :returning [:*]}))))

(defn- provision! [db user]
  (tenant-svc/provision-tenant! db {:tenant-defaults {:payer-types [] :expense-categories []}} user))

(defn- add-member! [db tenant-id owner-id invitee role]
  "Invite and accept to create a membership."
  (let [inv (invitation-svc/create-invitation! db
              {:tenant-id  tenant-id
               :email      (or (:email invitee) (:users/email invitee))
               :role       role
               :invited-by owner-id})
        token (or (:token inv) (:tenant_invitations/token inv))
        full-inv (invitation-svc/find-invitation-by-token db token)]
    (invitation-svc/accept-invitation! db invitee full-inv)))

(defn- insert-membership!
  [db {:keys [tenant-id user-id role status]}]
  (let [now (java.time.LocalDateTime/now)]
    (jdbc/execute-one! db
      (sql/format {:insert-into [:tenant_memberships]
                   :values [{:id         (java.util.UUID/randomUUID)
                             :tenant_id  tenant-id
                             :user_id    user-id
                             :role       [:cast role :membership_role]
                             :status     [:cast (or status "active") :membership_status]
                             :created_at now
                             :updated_at now}]
                   :returning [:*]}))))

(defn- force-constraint-check!
  [db]
  (jdbc/execute! db ["SET CONSTRAINTS ALL IMMEDIATE"]))

;; ============================================================================
;; change-role!
;; ============================================================================

(deftest change-role-happy-path
  (let [db       fixtures/*test-db*
        owner    (create-user! db "cr-owner")
        member   (create-user! db "cr-member")
        {:keys [tenant membership]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))
        member-m  (add-member! db tenant-id owner-id member "member")
        result    (member-svc/change-role! db {:actor-membership  membership
                                               :target-membership member-m
                                               :new-role          "viewer"})]

    (testing "changes the role"
      (is (= "viewer" (or (:role result) (:tenant_memberships/role result)))))))

(deftest superpower-change-role-happy-path
  (let [db       fixtures/*test-db*
        owner    (create-user! db "sp-cr-owner")
        member   (create-user! db "sp-cr-member")
        {:keys [tenant membership]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))
        member-m  (add-member! db tenant-id owner-id member "member")
        result    (member-svc/superpower-change-role!
                    db
                    {:target-membership member-m
                     :new-role          "admin"})]

    (testing "platform-admin flow can promote a member to admin"
      (is (= "admin" (or (:role result) (:tenant_memberships/role result)))))))

(deftest change-role-cannot-change-own
  (let [db    fixtures/*test-db*
        owner (create-user! db "cr-self")
        {:keys [membership]} (provision! db owner)]

    (testing "rejects self role change"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Cannot change your own role"
            (member-svc/change-role! db {:actor-membership  membership
                                         :target-membership membership
                                         :new-role          "admin"}))))))

(deftest change-role-non-owner-cannot-promote-to-admin
  (let [db       fixtures/*test-db*
        owner    (create-user! db "cr-promote-owner")
        admin    (create-user! db "cr-promote-admin")
        member   (create-user! db "cr-promote-member")
        {:keys [tenant membership]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))
        admin-m  (add-member! db tenant-id owner-id admin "admin")
        member-m (add-member! db tenant-id owner-id member "member")]

    (testing "admin cannot promote to admin"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Only owners"
            (member-svc/change-role! db {:actor-membership  admin-m
                                         :target-membership member-m
                                         :new-role          "admin"}))))))

;; ============================================================================
;; transfer-ownership!
;; ============================================================================

(deftest transfer-ownership-happy-path
  (let [db       fixtures/*test-db*
        owner    (create-user! db "xfer-owner")
        admin    (create-user! db "xfer-admin")
        {:keys [tenant membership]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))
        admin-m  (add-member! db tenant-id owner-id admin "admin")
        _result  (member-svc/transfer-ownership! db {:actor-membership  membership
                                                     :target-membership admin-m})]

    (testing "former owner is now admin"
      (let [owner-m-after (tenant-svc/get-membership db tenant-id owner-id)]
        (is (= "admin" (or (:role owner-m-after) (:tenant_memberships/role owner-m-after))))))

    (testing "target is now owner"
      (let [admin-id (or (:id admin) (:users/id admin))
            admin-m-after (tenant-svc/get-membership db tenant-id admin-id)]
        (is (= "owner" (or (:role admin-m-after) (:tenant_memberships/role admin-m-after))))))))

(deftest transfer-ownership-target-not-admin
  (let [db       fixtures/*test-db*
        owner    (create-user! db "xfer-not-admin-owner")
        member   (create-user! db "xfer-not-admin-member")
        {:keys [tenant membership]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))
        member-m (add-member! db tenant-id owner-id member "member")]

    (testing "rejects transfer to non-admin"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"admin"
            (member-svc/transfer-ownership! db {:actor-membership  membership
                                                :target-membership member-m}))))))

;; ============================================================================
;; remove-member!
;; ============================================================================

(deftest remove-member-happy-path
  (let [db       fixtures/*test-db*
        owner    (create-user! db "rm-owner")
        member   (create-user! db "rm-member")
        {:keys [tenant membership]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))
        member-m (add-member! db tenant-id owner-id member "member")
        result   (member-svc/remove-member! db {:actor-membership  membership
                                                :target-membership member-m})]

    (testing "sets status to suspended"
      (is (= "suspended" (or (:status result) (:tenant_memberships/status result)))))))

(deftest superpower-remove-member-happy-path
  (let [db       fixtures/*test-db*
        owner    (create-user! db "sp-rm-owner")
        member   (create-user! db "sp-rm-member")
        {:keys [tenant membership]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))
        member-m  (add-member! db tenant-id owner-id member "member")
        result    (member-svc/superpower-remove-member!
                    db
                    {:target-membership member-m})]

    (testing "platform-admin flow can suspend a non-owner membership"
      (is (= "suspended" (or (:status result) (:tenant_memberships/status result)))))))

(deftest db-forbids-suspending-or-demoting-the-last-owner
  (let [db    fixtures/*test-db*
        owner (create-user! db "db-last-owner")
        {:keys [membership]} (provision! db owner)
        owner-membership-id (or (:id membership) (:tenant_memberships/id membership))]

    (testing "forcing the only owner away from owner fails when deferred constraints are checked"
      (is (thrown-with-msg?
            org.postgresql.util.PSQLException
            #"exactly one active owner"
            (do
              (jdbc/execute-one! db
                (sql/format {:update [:tenant_memberships]
                             :set    {:role [:cast "admin" :membership_role]}
                             :where  [:= :id owner-membership-id]}))
              (force-constraint-check! db)))))))

(deftest db-forbids-two-active-owners-for-one-tenant
  (let [db       fixtures/*test-db*
        owner    (create-user! db "db-owner")
        admin    (create-user! db "db-admin")
        {:keys [tenant membership]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        admin-id  (or (:id admin) (:users/id admin))]

    (testing "partial unique index blocks a second active owner"
      (is (thrown? org.postgresql.util.PSQLException
            (insert-membership! db {:tenant-id tenant-id
                                    :user-id   admin-id
                                    :role      "owner"}))))))

(deftest remove-member-cannot-remove-owner
  (let [db       fixtures/*test-db*
        owner    (create-user! db "rm-cant-owner")
        admin    (create-user! db "rm-cant-admin")
        {:keys [tenant membership]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))
        admin-m  (add-member! db tenant-id owner-id admin "admin")]

    (testing "rejects removing the owner"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Cannot remove the tenant owner"
            (member-svc/remove-member! db {:actor-membership  admin-m
                                           :target-membership membership}))))))
