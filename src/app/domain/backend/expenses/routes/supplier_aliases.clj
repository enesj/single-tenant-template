(ns app.domain.backend.expenses.routes.supplier-aliases
  "Admin API routes for supplier aliases."
  (:require
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/supplier-alias-config
                 (factory/register-entity-routes!))]
    (factory/build-standard-routes db config)))
