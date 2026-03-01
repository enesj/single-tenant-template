(ns app.domain.backend.expenses.services.tenant-isolation-test
  "Integration tests verifying tenant data isolation.

  These tests create two tenants and verify that:
  - Payers created in tenant A are invisible to tenant B
  - Default payer/payer-type in tenant A does not affect tenant B
  - User expense settings are per-tenant per-user
  - Expenses created in tenant A are invisible when querying tenant B
  - Global catalog (suppliers) remains visible to both tenants"
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.payers :as payers]
    [app.domain.backend.expenses.services.payer-types :as payer-types]
    [app.domain.backend.expenses.services.user-expense-settings :as settings]
    [app.domain.backend.expenses.services.user-expenses :as user-expenses]
    [app.domain.expenses.test-helpers :as th]
    [clojure.test :refer [deftest is testing use-fixtures]])
  (:import
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

;; ---------------------------------------------------------------------------
;; Payer isolation
;; ---------------------------------------------------------------------------

(deftest payers-isolated-by-tenant
  (testing "payers created in tenant A are invisible when listing tenant B's payers"
    (let [db fixtures/*test-db*
          user-a (th/ensure-test-user! db {:email "alice@tenant-a.com"})
          user-b (th/ensure-test-user! db {:email "bob@tenant-b.com"})
          {:keys [tenant-id] :as _ta} (th/ensure-test-tenant! db user-a)
          tenant-a-id tenant-id
          {:keys [tenant-id] :as _tb} (th/ensure-test-tenant! db user-b)
          tenant-b-id tenant-id
          ;; Create a payer in tenant A
          payer-a (th/create-payer! db {:type "cash"
                                        :label (str "A-cash-" (UUID/randomUUID))
                                        :is_default true
                                        :tenant_id tenant-a-id})
          ;; List payers scoped to tenant B
          list-payers (:list payers/service)
          payers-in-b (list-payers db {:tenant-id tenant-b-id :limit 100 :offset 0})
          payer-b-ids (set (map :id payers-in-b))]
      (is (some? (:id payer-a))
        "Payer should be created successfully in tenant A")
      (is (not (contains? payer-b-ids (:id payer-a)))
        "Tenant B should not see tenant A's payer"))))

(deftest default-payer-isolated-by-tenant
  (testing "setting default payer in tenant A does not affect tenant B"
    (let [db fixtures/*test-db*
          user-a (th/ensure-test-user! db {:email "alice2@tenant-a.com"})
          user-b (th/ensure-test-user! db {:email "bob2@tenant-b.com"})
          {:keys [tenant-id] :as _ta} (th/ensure-test-tenant! db user-a)
          tenant-a-id tenant-id
          {:keys [tenant-id] :as _tb} (th/ensure-test-tenant! db user-b)
          tenant-b-id tenant-id
          ;; Create default payer in tenant A
          _payer-a (th/create-payer! db {:type "cash"
                                         :label (str "A-default-" (UUID/randomUUID))
                                         :is_default true
                                         :tenant_id tenant-a-id})
          ;; Query default payer for each tenant
          default-a (payers/get-default-payer db tenant-a-id)
          default-b (payers/get-default-payer db tenant-b-id)]
      (is (some? default-a)
        "Tenant A should have a default payer")
      (is (nil? default-b)
        "Tenant B should have no default payer (none created)"))))

;; ---------------------------------------------------------------------------
;; Payer-type isolation
;; ---------------------------------------------------------------------------

(deftest payer-types-isolated-by-tenant
  (testing "payer types created in tenant A are invisible to tenant B"
    (let [db fixtures/*test-db*
          user-a (th/ensure-test-user! db {:email "pt-alice@tenant-a.com"})
          user-b (th/ensure-test-user! db {:email "pt-bob@tenant-b.com"})
          {:keys [tenant-id] :as _ta} (th/ensure-test-tenant! db user-a)
          tenant-a-id tenant-id
          {:keys [tenant-id] :as _tb} (th/ensure-test-tenant! db user-b)
          tenant-b-id tenant-id
          ;; Create payer type in tenant A
          pt-a (payer-types/create-payer-type!
                 db
                 {:label (str "A-type-" (UUID/randomUUID))
                  :is_default false
                  :tenant_id tenant-a-id})
          ;; List payer types for tenant B
          list-pts (:list payer-types/service)
          pts-in-b (list-pts db {:tenant-id tenant-b-id :limit 100 :offset 0})
          pt-b-ids (set (map :id pts-in-b))]
      (is (some? (:id pt-a))
        "Payer type should be created in tenant A")
      (is (not (contains? pt-b-ids (:id pt-a)))
        "Tenant B should not see tenant A's payer type"))))

;; ---------------------------------------------------------------------------
;; User expense settings isolation
;; ---------------------------------------------------------------------------

(deftest user-expense-settings-isolated-by-tenant
  (testing "user expense settings are per-tenant per-user"
    (let [db fixtures/*test-db*
          user (th/ensure-test-user! db {:email "settings@shared.com"})
          user-id (:id user)
          {:keys [tenant-id] :as _ta} (th/ensure-test-tenant! db user)
          tenant-a-id tenant-id
          ;; Create a second tenant (same user can be in multiple tenants)
          user-b (th/ensure-test-user! db {:email "settings-owner-b@shared.com"})
          {:keys [tenant-id] :as _tb} (th/ensure-test-tenant! db user-b)
          tenant-b-id tenant-id
          ;; Save settings in tenant A
          _saved (settings/upsert-user-expense-settings!
                   db tenant-a-id user-id
                   {:default-currency "EUR"
                    :notifications-enabled true
                    :auto-post-after-upload-enabled false
                    :receipt-refine-enabled false})
          ;; Read settings from each tenant
          settings-a (settings/get-user-expense-settings db tenant-a-id user-id)
          settings-b (settings/get-user-expense-settings db tenant-b-id user-id)]
      (is (some? settings-a)
        "Settings should exist for user in tenant A")
      (is (= "EUR" (:default-currency settings-a))
        "Tenant A settings should have EUR currency")
      (is (nil? settings-b)
        "Tenant B should have no settings for this user"))))

;; ---------------------------------------------------------------------------
;; Expense isolation
;; ---------------------------------------------------------------------------

(deftest expenses-isolated-by-tenant
  (testing "expenses created in tenant A are invisible when listing tenant B's expenses"
    (let [db fixtures/*test-db*
          user (th/ensure-test-user! db {:email "exp-user@shared.com"})
          user-id (:id user)
          {:keys [tenant-id] :as _ta} (th/ensure-test-tenant! db user)
          tenant-a-id tenant-id
          user-b (th/ensure-test-user! db {:email "exp-owner-b@shared.com"})
          {:keys [tenant-id] :as _tb} (th/ensure-test-tenant! db user-b)
          tenant-b-id tenant-id
          ;; Create payer + supplier in tenant A (required FKs)
          payer-a (th/create-payer! db {:type "cash"
                                        :label (str "exp-payer-" (UUID/randomUUID))
                                        :is_default false
                                        :tenant_id tenant-a-id})
          create-supplier! (:create! @(requiring-resolve 'app.domain.backend.expenses.services.suppliers/service))
          supplier (create-supplier! db {:display_name (str "Supplier-" (UUID/randomUUID))})
          ;; Create an expense in tenant A
          expense-a (user-expenses/create-user-expense!
                      db tenant-a-id user-id
                      {:payer_id (:id payer-a)
                       :supplier_id (:id supplier)
                       :purchased_at "2026-01-15"
                       :total_amount 42.50M
                       :currency "EUR"
                       :notes "Tenant A expense"}
                      [{:raw_label "Test item"
                        :line_total 42.50M}])
          ;; List expenses for tenant B
          expenses-b (user-expenses/list-user-expenses
                       db tenant-b-id user-id
                       {:limit 100 :offset 0})
          ;; List expenses for tenant A (should include the expense)
          expenses-a (user-expenses/list-user-expenses
                       db tenant-a-id user-id
                       {:limit 100 :offset 0})]
      (is (some? (:id expense-a))
        "Expense should be created in tenant A")
      (is (empty? expenses-b)
        "Tenant B should see no expenses for this user")
      (is (= 1 (count expenses-a))
        "Tenant A should see exactly 1 expense"))))

;; ---------------------------------------------------------------------------
;; Global catalog remains shared
;; ---------------------------------------------------------------------------

(deftest global-catalog-visible-to-all-tenants
  (testing "suppliers (global catalog) are visible regardless of tenant"
    (let [db fixtures/*test-db*
          create-supplier! (:create! @(requiring-resolve 'app.domain.backend.expenses.services.suppliers/service))
          list-suppliers @(requiring-resolve 'app.domain.backend.expenses.services.suppliers/list-suppliers)
          supplier (create-supplier! db {:display_name (str "Global Supplier " (UUID/randomUUID))})
          ;; List suppliers without any tenant filter
          all-suppliers (list-suppliers db {:limit 100 :offset 0})
          supplier-ids (set (map :id all-suppliers))]
      (is (some? (:id supplier))
        "Supplier should be created successfully")
      (is (contains? supplier-ids (:id supplier))
        "Supplier should be visible in global listing"))))
