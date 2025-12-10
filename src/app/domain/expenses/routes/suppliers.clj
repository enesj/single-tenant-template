(ns app.domain.expenses.routes.suppliers
  "Admin API routes for expense suppliers."
  (:require
    [app.domain.expenses.routes.routes-factory :as factory]
    [app.domain.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/supplier-config
                    (factory/register-entity-routes!))]
    (factory/build-extended-routes db config)))