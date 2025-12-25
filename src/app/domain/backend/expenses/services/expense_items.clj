(ns app.domain.backend.expenses.services.expense-items
  "Expense item CRUD services using the factory pattern.

   Expense items are the line items attached to an expense (expense_items table).
   This service exists to support a standalone admin CRUD endpoint."
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]))

;; ==========================================================================
;; Service Registration
;; ==========================================================================

(def config (configs/get-entity-config :expense-item))

;; ==========================================================================
;; Generated CRUD Operations
;; ==========================================================================

(def service (factory/build-entity-service config))

;; Legacy function names for backward compatibility with routes
(def list-expense-items (:list service))
(def get-expense-item (:get service))
(def create-expense-item! (:create! service))
(def update-expense-item! (:update! service))
(def delete-expense-item! (:delete! service))
(def count-expense-items (:count service))
(def search-expense-items (:search service))
