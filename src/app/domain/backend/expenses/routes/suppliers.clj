(ns app.domain.backend.expenses.routes.suppliers
  "Admin API routes for expense suppliers."
  (:require
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/supplier-config
                    (factory/register-entity-routes!))]
    (factory/build-extended-routes db config)))