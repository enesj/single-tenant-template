(ns app.domain.backend.expenses.routes.cities
  "Admin API routes for cities."
  (:require
    [app.domain.backend.expenses.routes.route-configs :as configs]
    [app.domain.backend.expenses.routes.routes-factory :as factory]))

(defn routes
  [db]
  (let [config (-> configs/city-config
                 (factory/register-entity-routes!))]
    (factory/build-extended-routes db config)))
