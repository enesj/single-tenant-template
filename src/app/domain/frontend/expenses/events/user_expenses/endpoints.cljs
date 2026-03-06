(ns app.domain.frontend.expenses.events.user-expenses.endpoints
  "API endpoints for user-facing expense operations."
  (:require
    [app.template.frontend.api :as api]))

(def summary-endpoint (api/versioned-endpoint "/expenses/summary"))
(def list-endpoint (api/versioned-endpoint "/expenses"))
(def by-month-endpoint (api/versioned-endpoint "/expenses/by-month"))
(def by-supplier-endpoint (api/versioned-endpoint "/expenses/by-supplier"))
(def expense-detail-endpoint (api/versioned-endpoint "/expenses"))

(def reports-endpoint (api/versioned-endpoint "/expenses/reports"))
(def reports-day-of-week-endpoint (str reports-endpoint "/day-of-week"))
(def reports-size-distribution-endpoint (str reports-endpoint "/size-distribution"))
(def reports-daily-heatmap-endpoint (str reports-endpoint "/daily-heatmap"))
(def reports-filter-options-endpoint (str reports-endpoint "/filter-options"))

(def suppliers-endpoint (api/versioned-endpoint "/expenses/suppliers"))

(def stores-endpoint (api/versioned-endpoint "/expenses/stores"))

(def stores-batch-endpoint (str stores-endpoint "/batch"))
(def store-aliases-endpoint (api/versioned-endpoint "/expenses/store-aliases"))
(def payers-endpoint (api/versioned-endpoint "/expenses/payers"))
(def payer-types-endpoint (api/versioned-endpoint "/expenses/payer-types"))
(def expense-items-endpoint (api/versioned-endpoint "/expenses/expense-items"))
(def article-aliases-endpoint (api/versioned-endpoint "/expenses/article-aliases"))
(def supplier-aliases-endpoint (api/versioned-endpoint "/expenses/supplier-aliases"))

(def settings-endpoint (api/versioned-endpoint "/expenses/settings"))
(def upload-endpoint (api/versioned-endpoint "/expenses/upload"))

(def receipts-endpoint (api/versioned-endpoint "/expenses/receipts"))

;; Articles (role-gated in backend)
(def articles-endpoint (api/versioned-endpoint "/expenses/articles"))

;; Manufacturers (role-gated in backend)
(def manufacturers-endpoint (api/versioned-endpoint "/expenses/manufacturers"))

(def categories-endpoint (api/versioned-endpoint "/expenses/categories"))
(def expense-categories-endpoint (api/versioned-endpoint "/expenses/expense-categories"))
(def cities-endpoint (api/versioned-endpoint "/expenses/cities"))
(def subcategories-endpoint (api/versioned-endpoint "/expenses/subcategories"))
(def articles-unmapped-aliases-endpoint (api/versioned-endpoint "/expenses/articles/unmapped-aliases"))
