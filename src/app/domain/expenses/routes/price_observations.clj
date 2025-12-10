(ns app.domain.expenses.routes.price-observations
  "Admin API routes for price observations."
  (:require
    [app.domain.expenses.routes.routes-factory :as factory]
    [app.domain.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/price-observation-config
                    (factory/register-entity-routes!))]
    (factory/build-standard-routes db config)))