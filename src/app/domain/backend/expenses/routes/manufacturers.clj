(ns app.domain.backend.expenses.routes.manufacturers
  "Admin API routes for manufacturers."
  (:require
    [app.domain.backend.expenses.routes.route-configs :as configs]
    [app.domain.backend.expenses.routes.routes-factory :as factory]))

(defn routes [db]
  (let [config (-> configs/manufacturer-config
                 (factory/register-entity-routes!))]
    (factory/build-extended-routes db config)))
