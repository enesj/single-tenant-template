(ns app.template.frontend.events.user-expenses.endpoints
  (:require
    [app.template.frontend.api :as api]))

(def summary-endpoint (api/versioned-endpoint "/expenses/summary"))
(def list-endpoint (api/versioned-endpoint "/expenses"))
(def by-month-endpoint (api/versioned-endpoint "/expenses/by-month"))
(def by-supplier-endpoint (api/versioned-endpoint "/expenses/by-supplier"))
(def expense-detail-endpoint (api/versioned-endpoint "/expenses"))

(def suppliers-endpoint (api/versioned-endpoint "/expenses/suppliers"))
(def payers-endpoint (api/versioned-endpoint "/expenses/payers"))

(def admin-expenses-endpoint "/admin/api/expenses/entries")
(def admin-suppliers-endpoint "/admin/api/expenses/suppliers")
(def admin-payers-endpoint "/admin/api/expenses/payers")

