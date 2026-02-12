(ns app.domain.backend.expenses.routes.countries
  "Admin API routes for countries."
  (:require
    [app.domain.backend.expenses.routes.route-configs :as configs]
    [app.domain.backend.expenses.routes.routes-factory :as factory]))

(defn routes
  [db]
  (let [config (-> configs/country-config
                 (factory/register-entity-routes!))]
    (factory/build-extended-routes db config)))
