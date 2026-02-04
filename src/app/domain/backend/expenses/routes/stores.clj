(ns app.domain.backend.expenses.routes.stores
  "Admin API routes for stores (supplier locations)."
  (:require
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/store-config
                 (factory/register-entity-routes!))]
    (factory/build-extended-routes db config)))
