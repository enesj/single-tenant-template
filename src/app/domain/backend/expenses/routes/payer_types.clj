 (ns app.domain.backend.expenses.routes.payer-types
   "Admin API routes for payer types."
   (:require
     [app.domain.backend.expenses.routes.routes-factory :as factory]
     [app.domain.backend.expenses.routes.route-configs :as configs]))

 (defn routes [db]
   (let [config (-> configs/payer-type-config
                    (factory/register-entity-routes!))]
     (factory/build-standard-routes db config)))

