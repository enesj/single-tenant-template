(ns app.domain.frontend.expenses.admin.adapters.sync
  "Template sync events for expenses entities.

   These events sync data between admin/user contexts and the shared
   template entity store."
  (:require
    [app.admin.frontend.events.entity-sync :as entity-sync]
    [app.admin.frontend.events.users.template.form-interceptors :as form-interceptors]
    [app.domain.frontend.expenses.admin.adapters.normalize :as normalize]
    [app.template.frontend.shared.utils.entity :as entity-utils]))

;; =============================================================================
;; Template sync events
;; =============================================================================

(entity-utils/register-sync-event!
  {:event-id ::sync-expenses
   :entity-key :expenses
   :normalize-fn normalize/expense->template-entity
   :log-prefix "[expenses] Syncing expenses to template:"})

(entity-utils/register-upsert-event!
  {:event-id ::upsert-expenses
   :entity-key :expenses
   :normalize-fn normalize/expense->template-entity
   :log-prefix "[expenses] Upserting expenses to template:"})

(entity-utils/register-sync-event!
  {:event-id ::sync-expense-items
   :entity-key :expense-items
   :normalize-fn normalize/expense-item->template-entity
   :log-prefix "[expenses] Syncing expense items to template:"})

(entity-utils/register-sync-event!
  {:event-id ::sync-receipts
   :entity-key :receipts
   :normalize-fn normalize/receipt->template-entity
   :log-prefix "[expenses] Syncing receipts to template:"})

(entity-utils/register-sync-event!
  {:event-id ::sync-suppliers
   :entity-key :suppliers
   :normalize-fn normalize/supplier->template-entity
   :log-prefix "[expenses] Syncing suppliers to template:"})

(entity-utils/register-sync-event!
  {:event-id ::sync-manufacturers
   :entity-key :manufacturers
   :normalize-fn normalize/manufacturer->template-entity
   :log-prefix "[expenses] Syncing manufacturers to template:"})

(entity-utils/register-sync-event!
  {:event-id ::sync-payers
   :entity-key :payers
   :normalize-fn normalize/payer->template-entity
   :log-prefix "[expenses] Syncing payers to template:"})

(entity-utils/register-sync-event!
  {:event-id ::sync-payer-types
   :entity-key :payer-types
   :normalize-fn normalize/payer-type->template-entity
   :log-prefix "[expenses] Syncing payer types to template:"})

(entity-utils/register-sync-event!
  {:event-id ::sync-articles
   :entity-key :articles
   :normalize-fn normalize/article->template-entity
   :log-prefix "[expenses] Syncing articles to template:"})

(entity-utils/register-sync-event!
  {:event-id ::sync-article-aliases
   :entity-key :article-aliases
   :normalize-fn normalize/article-alias->template-entity
   :log-prefix "[expenses] Syncing article aliases to template:"})

(entity-utils/register-sync-event!
  {:event-id ::sync-supplier-aliases
   :entity-key :supplier-aliases
   :normalize-fn normalize/supplier-alias->template-entity
   :log-prefix "[expenses] Syncing supplier aliases to template:"})

(entity-utils/register-sync-event!
  {:event-id ::sync-price-observations
   :entity-key :price-observations
   :normalize-fn normalize/price-observation->template-entity
   :log-prefix "[expenses] Syncing price observations to template:"})

(entity-utils/register-sync-event!
  {:event-id ::sync-unmapped-aliases
   :entity-key :unmapped-aliases
   :normalize-fn normalize/unmapped-alias->template-entity
   :log-prefix "[expenses] Syncing unmapped aliases to template:"})

;; =============================================================================
;; Register with generic entity-sync dispatcher
;; =============================================================================
;; This enables the admin/refresh-entity-list event to dispatch to our sync events

(entity-sync/register-sync-handler! :expenses
  {:sync-event-id ::sync-expenses})
(entity-sync/register-sync-handler! :expense-items
  {:sync-event-id ::sync-expense-items})
(entity-sync/register-sync-handler! :receipts
  {:sync-event-id ::sync-receipts})
(entity-sync/register-sync-handler! :suppliers
  {:sync-event-id ::sync-suppliers})
(entity-sync/register-sync-handler! :manufacturers
  {:sync-event-id ::sync-manufacturers})
(entity-sync/register-sync-handler! :payers
  {:sync-event-id ::sync-payers})
(entity-sync/register-sync-handler! :payer-types
  {:sync-event-id ::sync-payer-types})
(entity-sync/register-sync-handler! :articles
  {:sync-event-id ::sync-articles})
(entity-sync/register-sync-handler! :article-aliases
  {:sync-event-id ::sync-article-aliases})
(entity-sync/register-sync-handler! :supplier-aliases
  {:sync-event-id ::sync-supplier-aliases})
(entity-sync/register-sync-handler! :price-observations
  {:sync-event-id ::sync-price-observations})
(entity-sync/register-sync-handler! :unmapped-aliases
  {:sync-event-id ::sync-unmapped-aliases})

;; =============================================================================
;; Register with form interceptors bridge
;; =============================================================================
;; This enables admin form submission to work with these entities

(form-interceptors/register-bridge-entity! :suppliers)
(form-interceptors/register-bridge-entity! :manufacturers)
(form-interceptors/register-bridge-entity! :expenses)
(form-interceptors/register-bridge-entity! :expense-items)
(form-interceptors/register-bridge-entity! :receipts)
(form-interceptors/register-bridge-entity! :payers)
(form-interceptors/register-bridge-entity! :payer-types)
(form-interceptors/register-bridge-entity! :articles)
(form-interceptors/register-bridge-entity! :article-aliases)
(form-interceptors/register-bridge-entity! :supplier-aliases)
(form-interceptors/register-bridge-entity! :price-observations)
