(ns app.domain.frontend.expenses.components.expense-form
  "Reusable expense form components using the template form system.

  Uses the master-detail-form wrapper for edit modal orchestration.

  This namespace is intentionally kept as a stable public entrypoint.
  Implementation is split into smaller namespaces under:
  `src/app/domain/frontend/expenses/components/expense_form/`."
  (:require
    [app.domain.frontend.expenses.components.expense-form.forms :as forms]
    [app.domain.frontend.expenses.components.expense-form.modals :as modals]
    [app.domain.frontend.expenses.components.expense-form.normalization :as norm]
    [app.domain.frontend.expenses.components.expense-form.specs :as specs]))

;; Re-export the original public API (backwards-compatible).

(def currency-options
  specs/currency-options)

(def line-item-columns
  specs/line-item-columns)

(def get-expense-form-spec
  specs/get-expense-form-spec)

(def normalize-receipt-data
  norm/normalize-receipt-data)

(def normalize-initial-data
  norm/normalize-initial-data)

(def validate-expense-values
  norm/validate-expense-values)

(def validate-receipt-review-values
  norm/validate-receipt-review-values)

(def prepare-expense-submit-values
  norm/prepare-expense-submit-values)

(def expense-form-body
  forms/expense-form-body)

(def receipt-approval-form
  forms/receipt-approval-form)

(def expense-add-form-modal
  modals/expense-add-form-modal)

(def expense-edit-form-modal
  modals/expense-edit-form-modal)
