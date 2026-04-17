(ns app.e2e.multi-tenancy.provisioning-test
  "E2E tests for tenant provisioning (Manual §2).

   Allium rules: RegisterAndProvision, OAuthRegisterAndProvision
   Covers: registration creates tenant, seeds categories/payers, slug from email, owner membership."
  (:require
    [app.e2e.fixtures :as fixtures]
    [app.e2e.helpers :as h]
    [clojure.test :refer [deftest is testing use-fixtures]]))

(use-fixtures :each fixtures/with-browser-context)

;; ---------------------------------------------------------------------------
;; Test: Registration auto-provisions a tenant
;; ---------------------------------------------------------------------------

(deftest register-creates-tenant
  (testing "Registering a new user auto-creates a tenant"
    (let [resp (h/api-register! h/user-a)]
      (is (= 200 (:status resp))
        "Registration should succeed with 200")
      (is (get-in resp [:body :success])
        "Response body should indicate success"))

    (testing "Tenant exists in DB"
      (let [tenant (h/get-tenant-by-slug "e2e-user-a")]
        (is (some? tenant)
          "Tenant should exist with slug derived from email prefix")
        (is (= "active" (:status tenant))
          "Tenant should be active")))))

;; ---------------------------------------------------------------------------
;; Test: Registration seeds default categories and payers
;; ---------------------------------------------------------------------------

(deftest register-seeds-categories-and-payers
  (testing "Registering creates seed data for the new tenant"
    ;; Register user-b so we have a fresh tenant to inspect
    (h/api-register! h/user-b)

    (testing "8 default expense categories are seeded"
      (let [categories (h/get-expense-categories-for-tenant "e2e-user-b")]
        (is (= 8 (count categories))
          "Should have 8 default expense categories")
        (let [names (set (map :name categories))]
          (is (contains? names "Groceries"))
          (is (contains? names "Transport"))
          (is (contains? names "Utilities"))
          (is (contains? names "Dining"))
          (is (contains? names "Healthcare"))
          (is (contains? names "Entertainment"))
          (is (contains? names "Shopping"))
          (is (contains? names "Other")))))

    (testing "starter payers plus the owner system payer are seeded"
      (let [payers (h/get-payers-for-tenant "e2e-user-b")
            labels (set (map :label payers))
            system-count (count (filter #(= "system" (:type %)) payers))
            custom-labels (set (map :label (filter #(= "custom" (:type %)) payers)))]
        (is (= 3 (count payers))
          "Should have 2 starter payers plus 1 owner system payer")
        (is (= 1 system-count)
          "Should create exactly one system payer for the tenant owner")
        (is (contains? labels "E2E User B")
          "Owner system payer should use the user's full name")
        (is (or (= #{"Household" "Other"} custom-labels)
              (= #{"Kućanstvo" "Ostalo"} custom-labels))
          "Custom payers should match the configured tenant starter payers")))))

;; ---------------------------------------------------------------------------
;; Test: Tenant slug is derived from email prefix
;; ---------------------------------------------------------------------------

(deftest tenant-slug-from-email
  (testing "Tenant slug is derived from the email prefix"
    ;; user-a should already be registered from register-creates-tenant,
    ;; but tests are independent so register if needed
    (h/api-register! h/user-a)
    (let [tenant (h/get-tenant-by-slug "e2e-user-a")]
      (is (some? tenant)
        "Tenant slug should be 'e2e-user-a' (email prefix)")
      (is (= "e2e-user-a" (:slug tenant))))))

;; ---------------------------------------------------------------------------
;; Test: Owner membership is created on registration
;; ---------------------------------------------------------------------------

(deftest owner-membership-created
  (testing "Registration creates an owner membership for the new user"
    (h/api-register! h/user-a)
    (let [memberships (h/get-memberships-for-user (:email h/user-a))]
      (is (= 1 (count memberships))
        "New user should have exactly 1 membership")
      (let [m (first memberships)]
        (is (= "owner" (:role m))
          "Membership role should be 'owner'")
        (is (= "active" (:status m))
          "Membership status should be 'active'")))))
