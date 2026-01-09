(ns app.domain.frontend.expenses.admin.components.detail-views
  "Re-exports all detail view components for backward compatibility.

  This file was split into smaller modules under detail_views/:
  - utils.cljs        - shared helpers (label-value, format-bytes, etc.)
  - supplier.cljs     - supplier-detail-body
  - article.cljs      - article-detail-body, article-add-aliases-modal
  - payer.cljs        - payer-detail-body
  - article_alias.cljs - article-alias-detail-body
  - price_observation.cljs - price-observation-detail-body
  - receipt.cljs      - receipt-detail-body, receipt-problem-alert
  - expense_item.cljs - expense-item-detail-body"
  (:require
    [app.domain.frontend.expenses.admin.components.detail-views.article :as article]
    [app.domain.frontend.expenses.admin.components.detail-views.article-alias :as article-alias]
    [app.domain.frontend.expenses.admin.components.detail-views.expense-item :as expense-item]
    [app.domain.frontend.expenses.admin.components.detail-views.payer :as payer]
    [app.domain.frontend.expenses.admin.components.detail-views.price-observation :as price-observation]
    [app.domain.frontend.expenses.admin.components.detail-views.receipt :as receipt]
    [app.domain.frontend.expenses.admin.components.detail-views.supplier :as supplier]))

;; Re-export all public components
(def supplier-detail-body supplier/supplier-detail-body)
(def article-detail-body article/article-detail-body)
(def article-add-aliases-modal article/article-add-aliases-modal)
(def payer-detail-body payer/payer-detail-body)
(def article-alias-detail-body article-alias/article-alias-detail-body)
(def price-observation-detail-body price-observation/price-observation-detail-body)
(def receipt-detail-body receipt/receipt-detail-body)
(def receipt-problem-alert receipt/receipt-problem-alert)
(def expense-item-detail-body expense-item/expense-item-detail-body)
