(ns app.template.backend.services.invitation-test
  "Tests for invitation create/accept/revoke guards."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.template.backend.security.tokens :as token-security]
    [app.template.backend.services.invitation :as invitation-svc]
    [app.template.backend.services.tenant :as tenant-svc]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

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
                             :email (str "inv-test-" suffix "-" id "@example.com")
                             :full_name (str "User " suffix)
                             :password_hash "placeholder"

                             :status [:cast "active" :user_status]
                             :auth_provider "password"
                             :email_verified false
                             :created_at now :updated_at now}]
                   :returning [:*]}))))

(defn- provision! [db user]
  (tenant-svc/provision-tenant! db {:tenant-defaults {:payers [] :expense-categories []}} user))

;; ============================================================================
;; create-invitation!
;; ============================================================================

(deftest create-invitation-happy-path
  (let [db    fixtures/*test-db*
        owner (create-user! db "owner")
        {:keys [tenant]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))
        inv (invitation-svc/create-invitation! db
              {:tenant-id  tenant-id
               :email      "newbie@example.com"
               :role       "member"
               :invited-by owner-id})]

    (testing "creates a pending invitation with raw token in the return value"
      (is (some? inv))
      (is (= "pending" (or (:status inv) (:tenant_invitations/status inv))))
      (is (some? (or (:token inv) (:tenant_invitations/token inv)))))

    (testing "stores only a hash of the invitation token"
      (let [raw-token (or (:token inv) (:tenant_invitations/token inv))
            stored (jdbc/execute-one! db
                     (sql/format {:select [:token]
                                  :from [:tenant_invitations]
                                  :where [:= :id (or (:id inv) (:tenant_invitations/id inv))]})
                     {:builder-fn rs/as-unqualified-maps})]
        (is (= (token-security/hash-token raw-token) (:token stored)))
        (is (not= raw-token (:token stored)))))))

(deftest create-invitation-invalid-role
  (let [db    fixtures/*test-db*
        owner (create-user! db "owner2")
        {:keys [tenant]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))]

    (testing "rejects invalid role"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid invitation role"
            (invitation-svc/create-invitation! db
              {:tenant-id tenant-id :email "x@x.com" :role "superuser" :invited-by owner-id}))))))

(deftest create-invitation-duplicate-pending
  (let [db    fixtures/*test-db*
        owner (create-user! db "owner3")
        {:keys [tenant]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))]

    ;; First invitation succeeds
    (invitation-svc/create-invitation! db
      {:tenant-id tenant-id :email "dup@example.com" :role "member" :invited-by owner-id})

    (testing "rejects duplicate pending invitation"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"pending invitation already exists"
            (invitation-svc/create-invitation! db
              {:tenant-id tenant-id :email "dup@example.com" :role "member" :invited-by owner-id}))))))

;; ============================================================================
;; accept-invitation!
;; ============================================================================

(deftest accept-invitation-happy-path
  (let [db       fixtures/*test-db*
        owner    (create-user! db "accept-owner")
        invitee  (create-user! db "accept-invitee")
        {:keys [tenant]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))
        inv      (invitation-svc/create-invitation! db
                   {:tenant-id  tenant-id
                    :email      (or (:email invitee) (:users/email invitee))
                    :role       "member"
                    :invited-by owner-id})
        token    (or (:token inv) (:tenant_invitations/token inv))
        full-inv (invitation-svc/find-invitation-by-token db token)
        membership (invitation-svc/accept-invitation! db invitee full-inv)]

    (testing "creates an active membership"
      (is (some? membership))
      (is (= "member" (or (:role membership) (:tenant_memberships/role membership)))))))

(deftest accept-invitation-wrong-email
  (let [db       fixtures/*test-db*
        owner    (create-user! db "wrong-email-owner")
        invitee  (create-user! db "wrong-email-invitee")
        {:keys [tenant]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))
        inv      (invitation-svc/create-invitation! db
                   {:tenant-id tenant-id :email "someone-else@example.com" :role "member" :invited-by owner-id})
        token    (or (:token inv) (:tenant_invitations/token inv))
        full-inv (invitation-svc/find-invitation-by-token db token)]

    (testing "rejects when email doesn't match"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"does not match"
            (invitation-svc/accept-invitation! db invitee full-inv))))))

;; ============================================================================
;; revoke-invitation!
;; ============================================================================

(deftest revoke-invitation-test
  (let [db    fixtures/*test-db*
        owner (create-user! db "revoke-owner")
        {:keys [tenant]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))
        inv (invitation-svc/create-invitation! db
              {:tenant-id tenant-id :email "revokee@example.com" :role "member" :invited-by owner-id})
        inv-id (or (:id inv) (:tenant_invitations/id inv))]

    (invitation-svc/revoke-invitation! db inv-id)

    (testing "invitation status is revoked"
      (let [fetched (invitation-svc/find-invitation-by-token db
                      (or (:token inv) (:tenant_invitations/token inv)))
            status (or (:status fetched) (:tenant_invitations/status fetched))]
        (is (= "revoked" status))))))

(deftest list-pending-invitations-does-not-expose-token-hashes
  (let [db    fixtures/*test-db*
        owner (create-user! db "token-list-owner")
        {:keys [tenant]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))]
    (invitation-svc/create-invitation! db
      {:tenant-id tenant-id :email "token-list@example.com" :role "member" :invited-by owner-id})
    (testing "list results omit token values entirely"
      (is (every? #(nil? (:token %))
            (invitation-svc/list-pending-invitations db tenant-id))))))

;; ============================================================================
;; list-pending-invitations
;; ============================================================================

(deftest list-pending-invitations-test
  (let [db    fixtures/*test-db*
        owner (create-user! db "list-owner")
        {:keys [tenant]} (provision! db owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-id  (or (:id owner) (:users/id owner))]

    (invitation-svc/create-invitation! db
      {:tenant-id tenant-id :email "a@example.com" :role "member" :invited-by owner-id})
    (invitation-svc/create-invitation! db
      {:tenant-id tenant-id :email "b@example.com" :role "admin" :invited-by owner-id})

    (testing "returns all pending invitations"
      (is (= 2 (count (invitation-svc/list-pending-invitations db tenant-id)))))))
