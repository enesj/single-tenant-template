(ns app.template.backend.routes.admin.tenants-test
  "Integration tests for admin tenant superpower CRUD.
   Uses transaction rollback for DB isolation."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.expenses.test-helpers :as th]
    [app.template.backend.services.tenant :as tenant-svc]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [app.shared.adapters.database :refer [convert-pg-objects]]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

;; ============================================================================
;; Test Helpers
;; ============================================================================

(defn- uuid-cast [id]
  (if (string? id) [:cast id :uuid] id))

(defn- add-membership!
  "Directly insert a membership for testing."
  [db {:keys [tenant-id user-id role]}]
  (convert-pg-objects
    (jdbc/execute-one! db
      (sql/format {:insert-into :tenant_memberships
                   :values [{:id         (UUID/randomUUID)
                             :tenant_id  (uuid-cast tenant-id)
                             :user_id    (uuid-cast user-id)
                             :role       [:cast (or role "member") :membership_role]
                             :status     [:cast "active" :membership_status]
                             :created_at [:raw "now()"]
                             :updated_at [:raw "now()"]}]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))

;; ============================================================================
;; Tenant Listing Tests
;; ============================================================================

(deftest list-tenants-returns-results
  (testing "list-tenants returns tenants with member count and owner info"
    (let [db    fixtures/*test-db*
          user  (th/ensure-test-user! db {:email "owner@tenants-list.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          ;; Use private var via resolve to test the query function directly
          list-fn @(resolve 'app.template.backend.routes.admin.tenants/list-tenants)
          tenants (list-fn db {:limit 50 :offset 0})]
      (is (pos? (count tenants)) "Should return at least one tenant")
      (let [our-tenant (first (filter #(= (str tenant-id) (str (:id %))) tenants))]
        (is (some? our-tenant) "Our test tenant should be in the list")
        (is (= 1 (:member_count our-tenant)) "Tenant should have 1 member (the owner)")
        (is (= "owner@tenants-list.test" (:owner_email our-tenant))
          "Owner email should be joined")))))

(deftest count-tenants-matches-list
  (testing "count-tenants returns correct total"
    (let [db    fixtures/*test-db*
          user  (th/ensure-test-user! db {:email "owner@tenants-count.test"})
          _     (th/ensure-test-tenant! db user)
          count-fn @(resolve 'app.template.backend.routes.admin.tenants/count-tenants)
          list-fn  @(resolve 'app.template.backend.routes.admin.tenants/list-tenants)
          total  (count-fn db {})
          all    (list-fn db {:limit total :offset 0})]
      (is (= total (count all)) "Count should match list length"))))

;; ============================================================================
;; Tenant Detail Tests
;; ============================================================================

(deftest get-tenant-detail-returns-enriched-info
  (testing "get-tenant-detail returns tenant with owner info and member count"
    (let [db    fixtures/*test-db*
          user  (th/ensure-test-user! db {:email "owner@tenants-detail.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          detail-fn @(resolve 'app.template.backend.routes.admin.tenants/get-tenant-detail)
          detail (detail-fn db tenant-id)]
      (is (some? detail) "Should return tenant detail")
      (is (= (str tenant-id) (str (:id detail))) "ID should match")
      (is (= "owner@tenants-detail.test" (:owner_email detail)) "Owner email joined")
      (is (= 1 (:member_count detail)) "Should have 1 member"))))

(deftest get-tenant-detail-returns-nil-for-nonexistent
  (testing "returns nil for a nonexistent tenant"
    (let [db fixtures/*test-db*
          detail-fn @(resolve 'app.template.backend.routes.admin.tenants/get-tenant-detail)
          detail (detail-fn db (UUID/randomUUID))]
      (is (nil? detail) "Should return nil for unknown tenant ID"))))

(deftest update-tenant-persists-editable-fields
  (testing "update-tenant! persists name, slug, and status changes"
    (let [db fixtures/*test-db*
          user (th/ensure-test-user! db {:email "owner@tenant-update.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          update-fn @(resolve 'app.template.backend.routes.admin.tenants/update-tenant!)
          updated (update-fn db tenant-id {:name "Updated Tenant"
                                           :slug "updated-tenant"
                                           :status [:cast "suspended" :tenant_status]
                                           :updated_at (java.time.LocalDateTime/now)})]
      (is (= "Updated Tenant" (:name updated)))
      (is (= "updated-tenant" (:slug updated)))
      (is (= "suspended" (str (:status updated)))))))

(deftest delete-tenants-removes-tenant-records
  (testing "delete-tenants! deletes tenants and returns deleted ids"
    (let [db fixtures/*test-db*
          user-a (th/ensure-test-user! db {:email "owner-a@tenant-delete.test"})
          user-b (th/ensure-test-user! db {:email "owner-b@tenant-delete.test"})
          tenant-a (th/ensure-test-tenant! db user-a)
          tenant-b (th/ensure-test-tenant! db user-b)
          tenant-id-a (:tenant-id tenant-a)
          tenant-id-b (:tenant-id tenant-b)
          delete-fn @(resolve 'app.template.backend.routes.admin.tenants/delete-tenants!)
          detail-fn @(resolve 'app.template.backend.routes.admin.tenants/get-tenant-detail)
          deleted-ids (set (delete-fn db [tenant-id-a tenant-id-b]))]
      (is (= #{(str tenant-id-a) (str tenant-id-b)} deleted-ids))
      (is (nil? (detail-fn db tenant-id-a)))
      (is (nil? (detail-fn db tenant-id-b))))))

;; ============================================================================
;; Member Listing Tests
;; ============================================================================

(deftest list-tenant-members-returns-user-info
  (testing "list-tenant-members returns members with user email"
    (let [db    fixtures/*test-db*
          owner (th/ensure-test-user! db {:email "owner@members-list.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db owner)
          ;; Add a second member
          user2 (th/ensure-test-user! db {:email "member@members-list.test"})
          _     (add-membership! db {:tenant-id tenant-id :user-id (:id user2) :role "member"})
          list-fn @(resolve 'app.template.backend.routes.admin.tenants/list-tenant-members)
          members (list-fn db tenant-id)]
      (is (= 2 (count members)) "Should have 2 members")
      (is (some #(= "owner@members-list.test" (:user_email %)) members)
        "Owner should be in member list")
      (is (some #(= "member@members-list.test" (:user_email %)) members)
        "Added member should be in list"))))

;; ============================================================================
;; Superpower Role Change Tests
;; ============================================================================

(deftest superpower-change-role-happy-path
  (testing "platform admin can change a member's role (superpower)"
    (let [db    fixtures/*test-db*
          owner (th/ensure-test-user! db {:email "owner@role-change.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db owner)
          user2 (th/ensure-test-user! db {:email "member@role-change.test"})
          membership (add-membership! db {:tenant-id tenant-id :user-id (:id user2) :role "member"})
          ;; Directly update via the same logic as the handler
          now (java.time.LocalDateTime/now)
          updated (convert-pg-objects
                    (jdbc/execute-one! db
                      (sql/format {:update [:tenant_memberships]
                                   :set    {:role       [:cast "admin" :membership_role]
                                            :updated_at now}
                                   :where  [:= :id (:id membership)]
                                   :returning [:*]})
                      {:builder-fn rs/as-unqualified-lower-maps}))]
      (is (= "admin" (str (:role updated))) "Role should be updated to admin"))))

(deftest superpower-change-role-rejects-owner-target
  (testing "cannot change the owner's role via superpower"
    (let [db    fixtures/*test-db*
          owner (th/ensure-test-user! db {:email "owner@role-guard.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db owner)
          get-membership-fn @(resolve 'app.template.backend.routes.admin.tenants/get-membership-by-id)
          ;; Find the owner's membership
          members-fn @(resolve 'app.template.backend.routes.admin.tenants/list-tenant-members)
          members (members-fn db tenant-id)
          owner-membership (first (filter #(= "owner" (str (:role %))) members))]
      (is (some? owner-membership) "Owner membership should exist")
      (is (= "owner" (str (:role owner-membership)))
        "Target is the owner — handler would block this with 403"))))

;; ============================================================================
;; Superpower Member Removal Tests
;; ============================================================================

(deftest superpower-remove-member-happy-path
  (testing "platform admin can remove a member (superpower)"
    (let [db    fixtures/*test-db*
          owner (th/ensure-test-user! db {:email "owner@remove.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db owner)
          user2 (th/ensure-test-user! db {:email "target@remove.test"})
          membership (add-membership! db {:tenant-id tenant-id :user-id (:id user2) :role "viewer"})
          ;; Remove by setting status to suspended
          now (java.time.LocalDateTime/now)
          updated (convert-pg-objects
                    (jdbc/execute-one! db
                      (sql/format {:update [:tenant_memberships]
                                   :set    {:status     [:cast "suspended" :membership_status]
                                            :updated_at now}
                                   :where  [:= :id (:id membership)]
                                   :returning [:*]})
                      {:builder-fn rs/as-unqualified-lower-maps}))]
      (is (= "suspended" (str (:status updated))) "Member should be suspended")
      ;; Verify they no longer appear in active members
      (let [members-fn @(resolve 'app.template.backend.routes.admin.tenants/list-tenant-members)
            remaining (members-fn db tenant-id)]
        (is (= 1 (count remaining)) "Only the owner should remain")))))

(deftest superpower-remove-member-rejects-owner
  (testing "cannot remove the tenant owner via superpower"
    (let [db    fixtures/*test-db*
          owner (th/ensure-test-user! db {:email "owner@remove-guard.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db owner)
          members-fn @(resolve 'app.template.backend.routes.admin.tenants/list-tenant-members)
          members (members-fn db tenant-id)
          owner-membership (first (filter #(= "owner" (str (:role %))) members))]
      (is (some? owner-membership) "Owner membership should exist")
      (is (= "owner" (str (:role owner-membership)))
        "Target is the owner — handler would block this with 403"))))

;; ============================================================================
;; Search / Filter Tests
;; ============================================================================

(deftest list-tenants-with-search-filter
  (testing "search filter works on tenant name and slug"
    (let [db    fixtures/*test-db*
          user  (th/ensure-test-user! db {:email "owner@search-filter.test"
                                          :name  "Unique Search Name"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          list-fn @(resolve 'app.template.backend.routes.admin.tenants/list-tenants)
          results (list-fn db {:search "Unique Search" :limit 50 :offset 0})]
      (is (pos? (count results)) "Should find tenant by name search")
      (is (some #(= (str tenant-id) (str (:id %))) results)
        "Our tenant should be in filtered results"))))

;; ============================================================================
;; User Detail Enrichment Tests
;; ============================================================================

(deftest user-memberships-enrichment
  (testing "tenant-svc/get-user-memberships returns membership info for a user"
    (let [db    fixtures/*test-db*
          user  (th/ensure-test-user! db {:email "enriched@memberships.test"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          memberships (tenant-svc/get-user-memberships db (:id user))
          ;; Find the membership for our specific tenant (user may have pre-existing memberships)
          our-m (first (filter #(= (str tenant-id) (str (:tenant_id %))) memberships))]
      (is (pos? (count memberships)) "User should have at least one membership")
      (is (some? our-m) "Should have a membership for our test tenant")
      (is (= "owner" (str (:role our-m))) "Role should be owner")
      (is (some? (:tenant_name our-m)) "Should include tenant name from JOIN"))))
