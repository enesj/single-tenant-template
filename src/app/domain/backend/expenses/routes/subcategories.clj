(ns app.domain.backend.expenses.routes.subcategories
  "Admin API routes for subcategories."
  (:require
    [app.domain.backend.expenses.routes.route-configs :as configs]
    [app.domain.backend.expenses.routes.routes-factory :as factory]))

(defn routes
  [db]
  (let [config (-> configs/subcategory-config
                 (factory/register-entity-routes!))]
    (factory/build-extended-routes db config)))


