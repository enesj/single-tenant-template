(ns app.domain.frontend.expenses.components.user-expense-form
  "User-facing expense modal entrypoints.

  Similar UX to the admin expenses modal forms, but wired to user-scoped
  events/endpoints (\"/api/v1/expenses\").

  Implementation is split into smaller namespaces under:
  `src/app/domain/frontend/expenses/components/user_expense_form/`."
  (:require
    [app.domain.frontend.expenses.components.user-expense-form.modals :as modals]))

;; Public modal entrypoints consumed by user pages.

(def user-expense-add-form-modal
  modals/user-expense-add-form-modal)

(def user-expense-edit-form-modal
  modals/user-expense-edit-form-modal)
