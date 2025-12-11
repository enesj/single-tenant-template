(ns app.domain.backend.expenses.routes.expenses
  "Admin API routes for expense records."
  (:require
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/expense-config
                    (factory/register-entity-routes!))]
    (factory/build-standard-routes db config)))