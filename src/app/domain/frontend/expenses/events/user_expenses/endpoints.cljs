(ns app.domain.frontend.expenses.events.user-expenses.endpoints
  "API endpoints for user-facing expense operations."
  (:require
    [app.template.frontend.api :as api]))

;; User-facing endpoints
(def summary-endpoint (api/versioned-endpoint "/expenses/summary"))
(def list-endpoint (api/versioned-endpoint "/expenses"))
(def by-month-endpoint (api/versioned-endpoint "/expenses/by-month"))
(def by-supplier-endpoint (api/versioned-endpoint "/expenses/by-supplier"))
(def expense-detail-endpoint (api/versioned-endpoint "/expenses"))

(def suppliers-endpoint (api/versioned-endpoint "/expenses/suppliers"))
(def payers-endpoint (api/versioned-endpoint "/expenses/payers"))
(def settings-endpoint (api/versioned-endpoint "/expenses/settings"))
(def upload-endpoint (api/versioned-endpoint "/expenses/upload"))

;; Receipts (user inbox)
(def receipts-endpoint (api/versioned-endpoint "/expenses/receipts"))

;; Articles (user app, role-gated in backend)
(def articles-endpoint (api/versioned-endpoint "/expenses/articles"))
(def articles-unmapped-items-endpoint (api/versioned-endpoint "/expenses/articles/unmapped-items"))

;; Admin endpoints for acting on behalf of a user
(def admin-expenses-endpoint "/admin/api/expenses/entries")
(def admin-receipts-endpoint "/admin/api/expenses/receipts")
(def admin-suppliers-endpoint "/admin/api/expenses/suppliers")
(def admin-payers-endpoint "/admin/api/expenses/payers")
(def admin-settings-endpoint "/admin/api/expenses/settings")
(def admin-upload-endpoint "/admin/api/expenses/upload")

;; Articles (admin)
(def admin-articles-endpoint "/admin/api/expenses/articles")
(def admin-articles-unmapped-items-endpoint "/admin/api/expenses/articles/unmapped-items")
