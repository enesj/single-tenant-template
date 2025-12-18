(ns app.domain.frontend.expenses.admin.adapters.sync
  "Template sync events for expenses entities.
   
   These events sync data between admin/user contexts and the shared
   template entity store."
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.events.entity-sync :as entity-sync]
    [app.admin.frontend.events.users.template.form-interceptors :as form-interceptors]
    [app.domain.frontend.expenses.admin.adapters.normalize :as normalize]))

;; =============================================================================
;; Template sync events
;; =============================================================================

(adapters.core/register-sync-event!
  {:event-id ::sync-expenses
   :entity-key :expenses
   :normalize-fn normalize/expense->template-entity
   :log-prefix "[expenses] Syncing expenses to template:"})

(adapters.core/register-upsert-event!
  {:event-id ::upsert-expenses
   :entity-key :expenses
   :normalize-fn normalize/expense->template-entity
   :log-prefix "[expenses] Upserting expenses to template:"})

(adapters.core/register-sync-event!
  {:event-id ::sync-receipts
   :entity-key :receipts
   :normalize-fn normalize/receipt->template-entity
   :log-prefix "[expenses] Syncing receipts to template:"})

(adapters.core/register-sync-event!
  {:event-id ::sync-suppliers
   :entity-key :suppliers
   :normalize-fn normalize/supplier->template-entity
   :log-prefix "[expenses] Syncing suppliers to template:"})

(adapters.core/register-sync-event!
  {:event-id ::sync-payers
   :entity-key :payers
   :normalize-fn normalize/payer->template-entity
   :log-prefix "[expenses] Syncing payers to template:"})

(adapters.core/register-sync-event!
  {:event-id ::sync-articles
   :entity-key :articles
   :normalize-fn normalize/article->template-entity
   :log-prefix "[expenses] Syncing articles to template:"})

(adapters.core/register-sync-event!
  {:event-id ::sync-article-aliases
   :entity-key :article-aliases
   :normalize-fn normalize/article-alias->template-entity
   :log-prefix "[expenses] Syncing article aliases to template:"})

(adapters.core/register-sync-event!
  {:event-id ::sync-price-observations
   :entity-key :price-observations
   :normalize-fn normalize/price-observation->template-entity
   :log-prefix "[expenses] Syncing price observations to template:"})

;; =============================================================================
;; Register with generic entity-sync dispatcher
;; =============================================================================
;; This enables the admin/refresh-entity-list event to dispatch to our sync events

(entity-sync/register-sync-handler! :expenses
  {:sync-event-id ::sync-expenses})
(entity-sync/register-sync-handler! :receipts
  {:sync-event-id ::sync-receipts})
(entity-sync/register-sync-handler! :suppliers
  {:sync-event-id ::sync-suppliers})
(entity-sync/register-sync-handler! :payers
  {:sync-event-id ::sync-payers})
(entity-sync/register-sync-handler! :articles
  {:sync-event-id ::sync-articles})
(entity-sync/register-sync-handler! :article-aliases
  {:sync-event-id ::sync-article-aliases})
(entity-sync/register-sync-handler! :price-observations
  {:sync-event-id ::sync-price-observations})

;; =============================================================================
;; Register with form interceptors bridge
;; =============================================================================
;; This enables admin form submission to work with these entities

(form-interceptors/register-bridge-entity! :suppliers)
(form-interceptors/register-bridge-entity! :expenses)
(form-interceptors/register-bridge-entity! :receipts)
(form-interceptors/register-bridge-entity! :payers)
(form-interceptors/register-bridge-entity! :articles)
(form-interceptors/register-bridge-entity! :article-aliases)
(form-interceptors/register-bridge-entity! :price-observations)
