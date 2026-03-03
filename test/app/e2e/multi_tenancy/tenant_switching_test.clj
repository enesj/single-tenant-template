(ns app.e2e.multi-tenancy.tenant-switching-test
  "E2E tests for tenant switching (Manual §7).

   Allium rules: SwitchTenant
   Allium surfaces: TenantSwitcher
   Covers: switcher visible for multi-tenant, hidden for single-tenant, switch changes data context, slug in URL."
  (:require
    [app.e2e.fixtures :as fixtures]
    [app.e2e.helpers :as h]
    [clojure.test :refer [deftest is testing use-fixtures]]))

(use-fixtures :each fixtures/with-browser-context)

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- setup-multi-tenant-user!
  "Register user-a (owner of tenant-a), register user-c (owner of tenant-c),
   then invite user-c to tenant-a as member. User-c becomes multi-tenant.
   Returns {:tenant-a <map> :tenant-c <map>}."
  []
  ;; Register user-a
  (h/api-register! h/user-a)

  ;; Invite user-c to user-a's tenant
  (h/api-invite-member! {:email (:email h/user-c) :role "member"})

  ;; Register user-c in separate context
  (let [ctx-c (.newContext (fixtures/get-browser))]
    (try
      (h/api-register! ctx-c h/user-c)
      ;; Accept invitation
      (let [token (h/get-invitation-token (:email h/user-c))]
        (h/api-accept-invitation! ctx-c token))
      (finally
        (.close ctx-c))))

  {:tenant-a (h/get-tenant-by-slug "e2e-user-a")
   :tenant-c (h/get-tenant-by-slug "e2e-user-c")})

;; ---------------------------------------------------------------------------
;; Test: Tenant switcher visible for multi-tenant user
;; ---------------------------------------------------------------------------

(deftest switcher-visible-for-multi-tenant
  (testing "Multi-tenant user has tenant switcher available"
    (setup-multi-tenant-user!)

    ;; Login as user-c (multi-tenant)
    (let [{:keys [body]} (h/api-login! h/user-c)]
      (is (:tenant-selection-required body)
        "Multi-tenant user should require tenant selection")
      (is (>= (count (:available-tenants body)) 2)
        "Should have 2+ available tenants"))

    ;; Fetch memberships — user-c should have 2
    (let [memberships (h/get-memberships-for-user (:email h/user-c))]
      (is (= 2 (count memberships))
        "User C should have 2 memberships"))))

;; ---------------------------------------------------------------------------
;; Test: Tenant switcher hidden for single-tenant user
;; ---------------------------------------------------------------------------

(deftest switcher-hidden-for-single-tenant
  (testing "Single-tenant user does NOT get tenant selection required"
    ;; Register user-b (single tenant)
    (h/api-register! h/user-b)

    (let [{:keys [body]} (h/api-auth-status)]
      (is (not (:tenant-selection-required body))
        "Single-tenant user should not require tenant selection")
      (is (some? (:tenant body))
        "Tenant should be auto-set for single-tenant user"))

    (let [memberships (h/get-memberships-for-user (:email h/user-b))]
      (is (= 1 (count memberships))
        "Single-tenant user should have exactly 1 membership"))))

;; ---------------------------------------------------------------------------
;; Test: Switching tenant changes data context
;; ---------------------------------------------------------------------------

(deftest switch-changes-data-context
  (testing "Switching tenant changes the data visible to the user"
    (let [{:keys [tenant-a tenant-c]} (setup-multi-tenant-user!)]

      ;; Login as user-c and select tenant-c first
      (h/api-login! h/user-c)
      (h/api-switch-tenant! (:id tenant-c))

      ;; Verify auth status shows tenant-c
      (let [{:keys [body]} (h/api-auth-status)]
        (is (= (str (:id tenant-c)) (get-in body [:tenant :id]))
          "Active tenant should be tenant-c"))

      ;; Switch to tenant-a
      (h/api-switch-tenant! (:id tenant-a))

      ;; Verify auth status now shows tenant-a
      (let [{:keys [body]} (h/api-auth-status)]
        (is (= (str (:id tenant-a)) (get-in body [:tenant :id]))
          "Active tenant should now be tenant-a")
        (is (= "member" (:membership-role body))
          "Role in tenant-a should be 'member' (the invited role)")))))

;; ---------------------------------------------------------------------------
;; Test: Slug in URL after switch
;; ---------------------------------------------------------------------------

(deftest slug-in-url-after-switch
  (testing "After switching tenants, the auth status reflects the new tenant slug"
    (let [{:keys [tenant-a tenant-c]} (setup-multi-tenant-user!)]

      ;; Login and select tenant-a
      (h/api-login! h/user-c)
      (h/api-switch-tenant! (:id tenant-a))

      (let [{:keys [body]} (h/api-auth-status)]
        (is (= "e2e-user-a" (get-in body [:tenant :slug]))
          "Active tenant slug should be 'e2e-user-a'"))

      ;; Switch to tenant-c
      (h/api-switch-tenant! (:id tenant-c))

      (let [{:keys [body]} (h/api-auth-status)]
        (is (= "e2e-user-c" (get-in body [:tenant :slug]))
          "Active tenant slug should be 'e2e-user-c'")))))
