(ns app.domain.backend.expenses.routes.price-observations
  "Admin API routes for price observations."
  (:require
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/price-observation-config
                    (factory/register-entity-routes!))]
    (factory/build-standard-routes db config)))
