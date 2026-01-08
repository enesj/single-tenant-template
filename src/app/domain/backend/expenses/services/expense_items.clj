(ns app.domain.backend.expenses.services.expense-items
  "Expense item CRUD services using the factory pattern.

   Expense items are the line items attached to an expense (expense_items table).
   This service exists to support a standalone admin CRUD endpoint."
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

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
(def delete-expense-item!
  "Soft delete an expense item by setting :deleted_at. Returns the updated row, or nil
  if it was already deleted or not found."
  (fn [db id]
    (jdbc/execute-one!
      db
      (sql/format {:update :expense_items
                   :set {:deleted_at [:now]}
                   :where [:and
                           [:= :id id]
                           [:is :deleted_at nil]]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))
(def count-expense-items (:count service))
(def search-expense-items (:search service))
