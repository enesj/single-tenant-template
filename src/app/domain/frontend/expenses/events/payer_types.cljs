 (ns app.domain.frontend.expenses.events.payer-types
   "Payer Types events - generated using the expenses event factory."
   (:require
     [app.domain.frontend.expenses.events.events-factory :as factory]
     [app.domain.frontend.expenses.events.entity-configs :as configs]))

 ;; Register standard CRUD events for payer types using the factory
 (factory/register-entity-events! configs/payer-types-config)

