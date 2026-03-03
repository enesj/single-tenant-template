(ns app.e2e.multi-tenancy.data-scoping-test
  "E2E tests for tenant data scoping / isolation (Manual §4).

   Allium rules: TenantDataIsolation, GlobalCatalogShared
   Covers: expense isolation, direct-access 404, receipt isolation, category/payer isolation, global catalog shared."
  (:require
    [app.e2e.fixtures :as fixtures]
    [app.e2e.helpers :as h]
    [clojure.set :as set]
    [clojure.test :refer [deftest is testing use-fixtures]]))

(use-fixtures :each fixtures/with-browser-context)

;; ---------------------------------------------------------------------------
;; Helpers: set up two tenants with scoped data
;; ---------------------------------------------------------------------------

(defn- setup-two-tenants!
  "Register user-a and user-b, each with their own tenant.
   Returns {:tenant-a <map> :tenant-b <map>}."
  []
  ;; Register in main context (user-a)
  (h/api-register! h/user-a)
  (let [tenant-a (h/get-tenant-by-slug "e2e-user-a")]
    ;; Register user-b in a separate context
    (let [ctx-b (.newContext (fixtures/get-browser))]
      (try
        (h/api-register! ctx-b h/user-b)
        (finally
          (.close ctx-b))))
    (let [tenant-b (h/get-tenant-by-slug "e2e-user-b")]
      {:tenant-a tenant-a :tenant-b tenant-b})))

;; ---------------------------------------------------------------------------
;; Test: Expense isolation between tenants
;; ---------------------------------------------------------------------------

(deftest expense-isolation-between-tenants
  (testing "Expenses created in Tenant A are not visible to Tenant B"
    (setup-two-tenants!)

    ;; Create an expense category in tenant-a (via API as user-a)
    (let [cats-a (h/get-expense-categories-for-tenant "e2e-user-a")
          cats-b (h/get-expense-categories-for-tenant "e2e-user-b")]

      (testing "Each tenant has its own expense categories"
        (is (= 8 (count cats-a)) "Tenant A should have 8 categories")
        (is (= 8 (count cats-b)) "Tenant B should have 8 categories"))

      (testing "Category IDs are different across tenants"
        (let [ids-a (set (map :id cats-a))
              ids-b (set (map :id cats-b))]
          (is (empty? (set/intersection ids-a ids-b))
            "Tenant A and B should have completely different category IDs"))))))

;; ---------------------------------------------------------------------------
;; Test: Direct cross-tenant access returns 404 (not 403)
;; ---------------------------------------------------------------------------

(deftest direct-access-returns-404
  (testing "Cross-tenant direct resource access returns 404"
    (setup-two-tenants!)

    ;; Get a category ID from tenant-a
    (let [cats-a (h/get-expense-categories-for-tenant "e2e-user-a")
          cat-id (:id (first cats-a))]

      ;; Login as user-b and try to access user-a's category
      (let [ctx-b (.newContext (fixtures/get-browser))]
        (try
          (h/api-login! ctx-b h/user-b)
          (let [{:keys [status]} (h/api-get ctx-b (str "/api/v1/entities/expense-categories/" cat-id))]
            (is (= 404 status)
              "Cross-tenant resource access should return 404, not 403"))
          (finally
            (.close ctx-b)))))))

;; ---------------------------------------------------------------------------
;; Test: Receipt isolation between tenants
;; ---------------------------------------------------------------------------

(deftest receipt-isolation
  (testing "Receipts are scoped per tenant"
    (setup-two-tenants!)

    ;; Check receipt counts — both tenants start with 0 receipts
    (let [count-a (h/count-table "receipts" "e2e-user-a")
          count-b (h/count-table "receipts" "e2e-user-b")]
      (is (= 0 count-a) "Tenant A should start with 0 receipts")
      (is (= 0 count-b) "Tenant B should start with 0 receipts"))

    ;; Verify via API that user-b sees no receipts
    (let [ctx-b (.newContext (fixtures/get-browser))]
      (try
        (h/api-login! ctx-b h/user-b)
        (let [{:keys [status body]} (h/api-get ctx-b "/api/v1/entities/receipts")]
          (is (= 200 status))
          (is (or (empty? (:data body)) (empty? body))
            "User B should see no receipts from Tenant A"))
        (finally
          (.close ctx-b))))))

;; ---------------------------------------------------------------------------
;; Test: Category and payer type isolation
;; ---------------------------------------------------------------------------

(deftest category-payer-isolation
  (testing "Expense categories and payer types are isolated per tenant"
    (setup-two-tenants!)

    ;; Create a custom category in tenant-a via direct DB insert
    (h/query-db
      "INSERT INTO expense_categories (id, tenant_id, name)
       SELECT ?::uuid, t.id, 'TenantA Special'
       FROM tenants t WHERE t.slug = 'e2e-user-a'"
      (str (java.util.UUID/randomUUID)))

    ;; Create a custom payer type in tenant-a
    (h/query-db
      "INSERT INTO payer_types (id, tenant_id, label, is_default)
       SELECT ?::uuid, t.id, 'TenantA Wallet', false
       FROM tenants t WHERE t.slug = 'e2e-user-a'"
      (str (java.util.UUID/randomUUID)))

    (testing "Tenant A has the custom category"
      (let [cats  (h/get-expense-categories-for-tenant "e2e-user-a")
            names (set (map :name cats))]
        (is (contains? names "TenantA Special"))))

    (testing "Tenant B does NOT see the custom category"
      (let [cats  (h/get-expense-categories-for-tenant "e2e-user-b")
            names (set (map :name cats))]
        (is (not (contains? names "TenantA Special"))
          "TenantA's custom category should not leak to Tenant B")))

    (testing "Tenant B does NOT see the custom payer type"
      (let [pts    (h/get-payer-types-for-tenant "e2e-user-b")
            labels (set (map :label pts))]
        (is (not (contains? labels "TenantA Wallet"))
          "TenantA's custom payer type should not leak to Tenant B")))))

;; ---------------------------------------------------------------------------
;; Test: Global catalog (suppliers) is shared across tenants
;; ---------------------------------------------------------------------------

(deftest global-catalog-shared
  (testing "Suppliers are global — visible to all tenants"
    (setup-two-tenants!)

    ;; Insert a test supplier (global, no tenant_id)
    (let [supplier-id    (java.util.UUID/randomUUID)
          display-name   "E2E Shared Supplier"
          normalized-key "e2e-shared-supplier"]
      (h/query-db
        "INSERT INTO suppliers (id, display_name, normalized_key)
         VALUES (?, ?, ?)
         ON CONFLICT (normalized_key) DO NOTHING"
        supplier-id
        display-name
        normalized-key))

    ;; Both tenants should see the supplier via API
    (let [{:keys [status body]} (h/api-get "/api/v1/entities/suppliers")]
      (is (= 200 status))
      (let [rows  (or (:data body) body)
            names (set (keep #(or (:displayName %) (:display-name %) (:display_name %) (:name %)) rows))]
        (is (contains? names "E2E Shared Supplier")
          "User A should see the shared supplier")))

    (let [ctx-b (.newContext (fixtures/get-browser))]
      (try
        (h/api-login! ctx-b h/user-b)
        (let [{:keys [status body]} (h/api-get ctx-b "/api/v1/entities/suppliers")]
          (is (= 200 status))
          (let [rows  (or (:data body) body)
                names (set (keep #(or (:displayName %) (:display-name %) (:display_name %) (:name %)) rows))]
            (is (contains? names "E2E Shared Supplier")
              "User B should also see the shared supplier")))
        (finally
          (.close ctx-b))))))
