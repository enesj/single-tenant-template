(ns app.template.backend.services.tenant-test
  "Tests for tenant provisioning, slug generation, and membership queries."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.template.backend.db.adapter :as db-adapter]
    [app.template.backend.services.tenant :as tenant-svc]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]))

(use-fixtures :each fixtures/with-transaction-rollback)

;; ============================================================================
;; Slug Generation (pure — no DB needed)
;; ============================================================================

(deftest generate-slug-test
  (testing "extracts prefix before @, lowercases, replaces non-alphanum with hyphens"
    (is (= "enes-bajric" (tenant-svc/generate-slug "enes.bajric@example.com")))
    (is (= "john" (tenant-svc/generate-slug "JOHN@example.com")))
    (is (= "user-123" (tenant-svc/generate-slug "user+123@gmail.com")))
    (is (= "a" (tenant-svc/generate-slug "a@b.c")))))

(deftest generate-slug-edge-cases
  (testing "trims leading/trailing hyphens"
    (is (= "name" (tenant-svc/generate-slug "-name-@test.com")))
    (is (= "test" (tenant-svc/generate-slug "...test...@foo.bar")))))

;; ============================================================================
;; ensure-unique-slug (requires DB)
;; ============================================================================

(deftest ensure-unique-slug-test
  (testing "returns base slug when not taken"
    (let [db fixtures/*test-db*]
      (is (= "free-slug" (tenant-svc/ensure-unique-slug db "free-slug")))))

  (testing "appends -2 when base slug is taken"
    (let [db fixtures/*test-db*]
      ;; Insert a tenant with the base slug
      (jdbc/execute-one! db
        (sql/format {:insert-into [:tenants]
                     :values [{:id (java.util.UUID/randomUUID)
                               :name "Test" :slug "taken"
                               :status [:cast "active" :tenant_status]
                               :created_at (java.time.LocalDateTime/now)
                               :updated_at (java.time.LocalDateTime/now)}]}))
      (is (= "taken-2" (tenant-svc/ensure-unique-slug db "taken"))))))

;; ============================================================================
;; generate-tenant-name
;; ============================================================================

(deftest generate-tenant-name-test
  (testing "uses full_name when present"
    (is (= "Enes's workspace"
          (tenant-svc/generate-tenant-name {:full_name "Enes" :email "e@x.com"}))))

  (testing "falls back to email prefix when no full_name"
    (is (= "user's workspace"
          (tenant-svc/generate-tenant-name {:full_name nil :email "user@example.com"})))))

;; ============================================================================
;; provision-tenant! (requires DB + config)
;; ============================================================================

(defn- create-test-user!
  "Insert a minimal user row and return the plain map."
  [db]
  (let [id (java.util.UUID/randomUUID)
        now (java.time.LocalDateTime/now)]
    (jdbc/execute-one! db
      (sql/format {:insert-into [:users]
                   :values [{:id id
                             :email (str "test-" id "@example.com")
                             :full_name "Test User"
                             :password_hash "placeholder"

                             :status [:cast "active" :user_status]
                             :auth_provider "password"
                             :email_verified false
                             :created_at now
                             :updated_at now}]
                   :returning [:*]}))))

(deftest provision-tenant-test
  (let [db   fixtures/*test-db*
        user (create-test-user! db)
        config {:tenant-defaults {:payer-types [{:label "Cash" :is-default true}
                                                {:label "Card" :is-default false}]
                                  :expense-categories [{:name "Groceries"}
                                                       {:name "Transport"}]}}
        result (tenant-svc/provision-tenant! db config user)]

    (testing "creates a tenant"
      (is (some? (:tenant result)))
      (is (= "active" (or (:status (:tenant result))
                        (:tenants/status (:tenant result))))))

    (testing "creates an owner membership"
      (is (some? (:membership result)))
      (is (= "owner" (or (:role (:membership result))
                       (:tenant_memberships/role (:membership result))))))

    (testing "seeds payer_types"
      (let [tenant-id (or (:id (:tenant result)) (:tenants/id (:tenant result)))
            pts (jdbc/execute! db
                  (sql/format {:select [:*]
                               :from [:payer_types]
                               :where [:= :tenant_id tenant-id]}))]
        (is (= 2 (count pts)))))

    (testing "seeds expense_categories"
      (let [tenant-id (or (:id (:tenant result)) (:tenants/id (:tenant result)))
            cats (jdbc/execute! db
                   (sql/format {:select [:*]
                                :from [:expense_categories]
                                :where [:= :tenant_id tenant-id]}))]
        (is (= 2 (count cats)))))))

;; ============================================================================
;; Membership Queries
;; ============================================================================

(deftest get-user-memberships-test
  (let [db     fixtures/*test-db*
        user   (create-test-user! db)
        config {:tenant-defaults {:payer-types [] :expense-categories []}}
        _      (tenant-svc/provision-tenant! db config user)
        user-id (or (:id user) (:users/id user))
        memberships (tenant-svc/get-user-memberships db user-id)]

    (testing "returns the newly created membership with tenant info"
      (is (= 1 (count memberships)))
      (is (some? (or (:tenant_name (first memberships))
                   (:tenant_memberships/tenant_name (first memberships))))))))

(deftest get-user-memberships-accepts-db-adapter-test
  (let [db        fixtures/*test-db*
        adapter   (db-adapter/create-postgres-adapter db)
        user      (create-test-user! db)
        config    {:tenant-defaults {:payer-types [] :expense-categories []}}
        _         (tenant-svc/provision-tenant! db config user)
        user-id   (or (:id user) (:users/id user))
        memberships (tenant-svc/get-user-memberships adapter user-id)]

    (testing "accepts the DI db-adapter record as well as raw jdbc connections"
      (is (= 1 (count memberships)))
      (is (some? (or (:tenant_name (first memberships))
                   (:tenant_memberships/tenant_name (first memberships))))))))

(deftest get-tenant-members-test
  (let [db     fixtures/*test-db*
        user   (create-test-user! db)
        config {:tenant-defaults {:payer-types [] :expense-categories []}}
        result (tenant-svc/provision-tenant! db config user)
        tenant-id (or (:id (:tenant result)) (:tenants/id (:tenant result)))
        members (tenant-svc/get-tenant-members db tenant-id)]

    (testing "returns the owner as a member with user info"
      (is (= 1 (count members)))
      (is (some? (or (:user_email (first members))
                   (:tenant_memberships/user_email (first members)))))
      (is (= "active" (or (:user_status (first members))
                        (:tenant_memberships/user_status (first members))))))))

(deftest get-tenant-members-include-suspended-test
  (let [db        fixtures/*test-db*
        owner     (create-test-user! db)
        config    {:tenant-defaults {:payer-types [] :expense-categories []}}
        result    (tenant-svc/provision-tenant! db config owner)
        tenant-id (or (:id (:tenant result)) (:tenants/id (:tenant result)))
        user2     (create-test-user! db)
        user2-id  (or (:id user2) (:users/id user2))
        now       (java.time.LocalDateTime/now)]

    ;; Insert a suspended membership row
    (jdbc/execute-one! db
      (sql/format {:insert-into [:tenant_memberships]
                   :values [{:id (java.util.UUID/randomUUID)
                             :tenant_id tenant-id
                             :user_id user2-id
                             :role [:cast "member" :membership_role]
                             :status [:cast "suspended" :membership_status]
                             :created_at now
                             :updated_at now}]}))

    (let [active-only (tenant-svc/get-tenant-members db tenant-id)
          with-suspended (tenant-svc/get-tenant-members db tenant-id {:include-suspended? true})]
      (testing "defaults to active memberships only"
        (is (= 1 (count active-only))))

      (testing "includes suspended memberships when requested"
        (is (= 2 (count with-suspended)))
        (is (some #(= "suspended" (or (:status %)
                                    (:tenant_memberships/status %)))
              with-suspended))))))

(deftest get-membership-test
  (let [db     fixtures/*test-db*
        user   (create-test-user! db)
        config {:tenant-defaults {:payer-types [] :expense-categories []}}
        result (tenant-svc/provision-tenant! db config user)
        tenant-id (or (:id (:tenant result)) (:tenants/id (:tenant result)))
        user-id   (or (:id user) (:users/id user))
        membership (tenant-svc/get-membership db tenant-id user-id)]

    (testing "returns the membership for a valid tenant+user"
      (is (some? membership))
      (is (= "owner" (or (:role membership)
                       (:tenant_memberships/role membership)))))

    (testing "returns nil for non-member"
      (is (nil? (tenant-svc/get-membership db tenant-id (java.util.UUID/randomUUID)))))))
