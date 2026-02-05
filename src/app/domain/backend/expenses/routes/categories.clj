(ns app.domain.backend.expenses.routes.categories
  "Admin API routes for categories."
  (:require
    [app.domain.backend.expenses.routes.route-configs :as configs]
    [app.domain.backend.expenses.routes.routes-factory :as factory]))

(defn routes
  [db]
  (let [config (-> configs/category-config
                 (factory/register-entity-routes!))]
    (factory/build-extended-routes db config)))


