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

;; NOTE: Avoid legacy alias vars like `list-expense-items`/`get-expense-item`.
;; Admin routes resolve operations via the `service` map, except for the custom
;; delete override below.
(def delete-expense-item!
  "Hard delete an expense item. Returns the deleted row, or nil if not found."
  (fn [db id]
    (jdbc/execute-one!
      db
      (sql/format {:delete-from :expense_items
                   :where [:= :id id]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))

