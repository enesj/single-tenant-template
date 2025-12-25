(ns app.domain.backend.expenses.routes.expense-items
  "Admin API routes for standalone expense items CRUD."
  (:require
    [app.domain.backend.expenses.routes.route-configs :as configs]
    [app.domain.backend.expenses.routes.routes-factory :as factory]))

(defn routes
  [db]
  (let [config (-> configs/expense-item-config
                 (factory/register-entity-routes!))]
    (factory/build-extended-routes db config)))
