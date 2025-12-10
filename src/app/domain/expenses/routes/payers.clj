(ns app.domain.expenses.routes.payers
  "Admin API routes for payers (payment sources)."
  (:require
    [app.domain.expenses.routes.routes-factory :as factory]
    [app.domain.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/payer-config
                    (factory/register-entity-routes!))]
    (factory/build-standard-routes db config)))