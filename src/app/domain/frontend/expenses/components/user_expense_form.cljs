(ns app.domain.frontend.expenses.components.user-expense-form
  "User-facing expense modal forms.

  Similar UX to the admin expenses modal forms, but wired to user-scoped
  events/endpoints (\"/api/v1/expenses\").

  This namespace is intentionally kept as a stable public entrypoint.
  Implementation is split into smaller namespaces under:
  `src/app/domain/frontend/expenses/components/user_expense_form/`."
  (:require
    [app.domain.frontend.expenses.components.user-expense-form.modals :as modals]))

;; Re-export the original public API (backwards-compatible).

(def user-expense-add-form-modal
  modals/user-expense-add-form-modal)

(def user-expense-edit-form-modal
  modals/user-expense-edit-form-modal)
