(ns app.domain.expenses.routes.expenses
  "Admin API routes for expense records."
  (:require
    [app.domain.expenses.routes.routes-factory :as factory]
    [app.domain.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/expense-config
                    (factory/register-entity-routes!))]
    (factory/build-standard-routes db config)))