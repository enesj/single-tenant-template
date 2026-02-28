(ns app.template.backend.auth.tenant-test
  "Tests for resolve-tenant-context dispatch (0/1/many memberships)."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.template.backend.auth.tenant :as tenant-auth]
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
                             :email (str "auth-tenant-" suffix "-" id "@example.com")
                             :full_name (str "User " suffix)
                             :password_hash "placeholder"
                             :role [:cast "member" :user_role]
                             :status [:cast "active" :user_status]
                             :auth_provider "password"
                             :email_verified false
                             :created_at now :updated_at now}]
                   :returning [:*]}))))

(def ^:private test-config
  {:tenant-defaults {:payer-types [{:label "Cash" :is-default true}]
                     :expense-categories [{:name "Other"}]}})

;; ============================================================================
;; resolve-tenant-context
;; ============================================================================

(deftest resolve-zero-memberships
  (let [db   fixtures/*test-db*
        user (create-user! db "zero")
        result (tenant-auth/resolve-tenant-context db test-config user)]

    (testing "provisions a new tenant"
      (is (= :provisioned (:action result)))
      (is (some? (:tenant result)))
      (is (= "owner" (or (get-in result [:membership :role])
                       (get-in result [:membership :tenant_memberships/role])))))))

(deftest resolve-one-membership
  (let [db   fixtures/*test-db*
        user (create-user! db "one")
        _    (tenant-svc/provision-tenant! db test-config user)
        result (tenant-auth/resolve-tenant-context db test-config user)]

    (testing "auto-sets the single tenant"
      (is (= :auto-set (:action result)))
      (is (some? (:tenant result))))))

(deftest resolve-multiple-memberships
  (let [db    fixtures/*test-db*
        user  (create-user! db "multi")
        _     (tenant-svc/provision-tenant! db test-config user)
        ;; Create a second tenant and add user as member
        user2 (create-user! db "multi-other")
        {:keys [tenant]} (tenant-svc/provision-tenant! db test-config user2)
        tenant2-id (or (:id tenant) (:tenants/id tenant))
        user-id    (or (:id user) (:users/id user))]
    ;; Manually insert a second membership
    (jdbc/execute-one! db
      (sql/format {:insert-into [:tenant_memberships]
                   :values [{:id         (java.util.UUID/randomUUID)
                             :tenant_id  tenant2-id
                             :user_id    user-id
                             :role       [:cast "member" :membership_role]
                             :status     [:cast "active" :membership_status]
                             :created_at (java.time.LocalDateTime/now)
                             :updated_at (java.time.LocalDateTime/now)}]}))

    (let [result (tenant-auth/resolve-tenant-context db test-config user)]
      (testing "returns selection-required with list of tenants"
        (is (= :selection-required (:action result)))
        (is (= 2 (count (:tenants result))))))))

;; ============================================================================
;; build-auth-session
;; ============================================================================

(deftest build-auth-session-provisioned
  (let [session (tenant-auth/build-auth-session
                  {:user {:id "u1" :email "x@x.com"}}
                  {:action :provisioned
                   :tenant {:id "t1" :name "Test"}
                   :membership {:id "m1" :role "owner"}})]
    (testing "includes tenant and membership"
      (is (= {:id "t1" :name "Test"} (:tenant session)))
      (is (= "owner" (get-in session [:membership :role]))))))

(deftest build-auth-session-selection-required
  (let [session (tenant-auth/build-auth-session
                  {:user {:id "u2"}}
                  {:action :selection-required
                   :tenants [{:tenant_id "t1" :tenant_name "A" :tenant_slug "a" :role "owner"}
                             {:tenant_id "t2" :tenant_name "B" :tenant_slug "b" :role "member"}]})]
    (testing "sets selection-required flag and available-tenants"
      (is (true? (:tenant-selection-required session)))
      (is (= 2 (count (:available-tenants session)))))))
