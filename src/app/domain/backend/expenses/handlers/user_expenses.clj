(ns app.domain.backend.expenses.handlers.user-expenses
  "API handlers for user-facing expense endpoints.

   These endpoints are mounted by the template API under /api/v1/expenses.
   All handlers extract user-id from the session and enforce user-based filtering.

   Implementation is split into focused submodules:
   - helpers: Common utilities (parsing, responses, auth)
   - crud: CRUD handlers for expenses
   - batch: Batch update/delete handlers
   - summary: Dashboard and summary handlers
   - reference-data: Suppliers and payers handlers
   - expense-items: Power-user expense items list"
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as helpers]
    [app.domain.backend.expenses.handlers.user-expenses.crud :as crud]
    [app.domain.backend.expenses.handlers.user-expenses.batch :as batch]
    [app.domain.backend.expenses.handlers.user-expenses.summary :as summary]
    [app.domain.backend.expenses.handlers.user-expenses.expense-items :as expense-items]
    [app.domain.backend.expenses.handlers.user-expenses.reference-data :as reference-data]))

;; Re-export helpers
(def try-parse-uuid helpers/try-parse-uuid)
(def get-user-id helpers/get-user-id)
(def json-response helpers/json-response)
(def unauthorized-response helpers/unauthorized-response)
(def not-found-response helpers/not-found-response)

;; Re-export CRUD handlers
(def list-expenses-handler crud/list-expenses-handler)
(def get-expense-handler crud/get-expense-handler)
(def create-expense-handler crud/create-expense-handler)
(def update-expense-handler crud/update-expense-handler)
(def delete-expense-handler crud/delete-expense-handler)

;; Re-export batch handlers
(def batch-update-expenses-handler batch/batch-update-expenses-handler)
(def batch-delete-expenses-handler batch/batch-delete-expenses-handler)

;; Re-export summary handlers
(def expense-summary-handler summary/expense-summary-handler)
(def spending-by-month-handler summary/spending-by-month-handler)
(def spending-by-supplier-handler summary/spending-by-supplier-handler)

;; Re-export reference data handlers
(def list-suppliers-handler reference-data/list-suppliers-handler)
(def get-supplier-handler reference-data/get-supplier-handler)
(def list-payers-handler reference-data/list-payers-handler)
(def create-supplier-handler reference-data/create-supplier-handler)
(def update-supplier-handler reference-data/update-supplier-handler)
(def delete-supplier-handler reference-data/delete-supplier-handler)
(def purge-supplier-preview-handler reference-data/purge-supplier-preview-handler)
(def purge-supplier-handler reference-data/purge-supplier-handler)
(def create-payer-handler reference-data/create-payer-handler)
(def update-payer-handler reference-data/update-payer-handler)
(def delete-payer-handler reference-data/delete-payer-handler)

;; Re-export expense items handlers
(def list-expense-items-handler expense-items/list-expense-items-handler)
(def update-expense-item-handler expense-items/update-expense-item-handler)
(def delete-expense-item-handler expense-items/delete-expense-item-handler)
