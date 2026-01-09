(ns app.domain.frontend.expenses.components.form-fields
  "Custom form fields for expense form

  Implementation is split into focused submodules:
  - helpers: Utility functions (parsing, formatting, line item helpers)
  - line-items: Line items input component
  - total-amount: Total amount input component
  - selects: Select components (supplier, article, expense)"
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers :as helpers]
    [app.domain.frontend.expenses.components.form-fields.line-items :as line-items]
    [app.domain.frontend.expenses.components.form-fields.total-amount :as total-amount]
    [app.domain.frontend.expenses.components.form-fields.selects :as selects]))

;; Re-export helpers
(def current-datetime-local helpers/current-datetime-local)
(def new-line-item helpers/new-line-item)
(def format-decimal helpers/format-decimal)
(def safe-parse-number helpers/safe-parse-number)
(def update-line-item helpers/update-line-item)
(def remove-line-item helpers/remove-line-item)
(def line-items-total helpers/line-items-total)

;; Re-export components
(def line-items-input line-items/line-items-input)
(def total-amount-input total-amount/total-amount-input)
(def supplier-select-with-inline-create selects/supplier-select-with-inline-create)
(def supplier-select-input selects/supplier-select-input)
(def article-select-input selects/article-select-input)
(def expense-select-input selects/expense-select-input)
