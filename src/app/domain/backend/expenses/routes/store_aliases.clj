(ns app.domain.backend.expenses.routes.store-aliases
  "Admin API routes for store aliases (deduplicated store/address guesses)."
  (:require
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/store-alias-config
                 (factory/register-entity-routes!))]
    (factory/build-extended-routes db config)))
