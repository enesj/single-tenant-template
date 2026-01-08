(ns app.domain.frontend.expenses.components.user-expense-form
  "User-facing expense modal forms.

  Similar UX to the admin expenses modal forms, but wired to user-scoped
  events/endpoints (\"/api/v1/expenses\").

  This namespace is intentionally kept as a stable public entrypoint.
  Implementation is split into smaller namespaces under:
  `src/app/domain/frontend/expenses/components/user_expense_form/`."
  (:require
    [app.domain.frontend.expenses.components.user-expense-form.forms :as forms]
    [app.domain.frontend.expenses.components.user-expense-form.inline-supplier-select :as inline-supplier-select]
    [app.domain.frontend.expenses.components.user-expense-form.modals :as modals]
    [app.domain.frontend.expenses.components.user-expense-form.normalization :as norm]
    [app.domain.frontend.expenses.components.user-expense-form.specs :as specs]))

;; Re-export the original public API (backwards-compatible).

(def user-supplier-select-with-inline-create
  inline-supplier-select/user-supplier-select-with-inline-create)

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

(def user-expense-form-body
  forms/user-expense-form-body)

(def receipt-approval-form
  forms/receipt-approval-form)

(def user-expense-add-form-modal
  modals/user-expense-add-form-modal)

(def user-expense-edit-form-modal
  modals/user-expense-edit-form-modal)
