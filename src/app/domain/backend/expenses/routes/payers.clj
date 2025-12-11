(ns app.domain.backend.expenses.routes.payers
  "Admin API routes for payers (payment sources)."
  (:require
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/payer-config
                    (factory/register-entity-routes!))]
    (factory/build-standard-routes db config)))