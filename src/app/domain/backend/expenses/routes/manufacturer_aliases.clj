(ns app.domain.backend.expenses.routes.manufacturer-aliases
  "Admin API routes for manufacturer aliases."
  (:require
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.routes.route-configs :as configs]))
(defn routes [db]
  (let [config (-> configs/manufacturer-alias-config
                 (factory/register-entity-routes!))]
    (factory/build-standard-routes db config)))
