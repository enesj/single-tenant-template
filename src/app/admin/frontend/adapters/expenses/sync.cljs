(ns app.admin.frontend.adapters.expenses.sync
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.adapters.expenses.normalize :as normalize]))

;; =============================================================================
;; Template sync events
;; =============================================================================

(adapters.core/register-sync-event!
  {:event-id :app.admin.frontend.adapters.expenses/sync-expenses
   :entity-key :expenses
   :normalize-fn normalize/expense->template-entity
   :log-prefix "[expenses] Syncing expenses to template:"})

(adapters.core/register-upsert-event!
  {:event-id :app.admin.frontend.adapters.expenses/upsert-expenses
   :entity-key :expenses
   :normalize-fn normalize/expense->template-entity
   :log-prefix "[expenses] Upserting expenses to template:"})

(adapters.core/register-sync-event!
  {:event-id :app.admin.frontend.adapters.expenses/sync-receipts
   :entity-key :receipts
   :normalize-fn normalize/receipt->template-entity
   :log-prefix "[expenses] Syncing receipts to template:"})

(adapters.core/register-sync-event!
  {:event-id :app.admin.frontend.adapters.expenses/sync-suppliers
   :entity-key :suppliers
   :normalize-fn normalize/supplier->template-entity
   :log-prefix "[expenses] Syncing suppliers to template:"})

(adapters.core/register-sync-event!
  {:event-id :app.admin.frontend.adapters.expenses/sync-payers
   :entity-key :payers
   :normalize-fn normalize/payer->template-entity
   :log-prefix "[expenses] Syncing payers to template:"})

(adapters.core/register-sync-event!
  {:event-id :app.admin.frontend.adapters.expenses/sync-articles
   :entity-key :articles
   :normalize-fn normalize/article->template-entity
   :log-prefix "[expenses] Syncing articles to template:"})

(adapters.core/register-sync-event!
  {:event-id :app.admin.frontend.adapters.expenses/sync-article-aliases
   :entity-key :article-aliases
   :normalize-fn normalize/article-alias->template-entity
   :log-prefix "[expenses] Syncing article aliases to template:"})

(adapters.core/register-sync-event!
  {:event-id :app.admin.frontend.adapters.expenses/sync-price-observations
   :entity-key :price-observations
   :normalize-fn normalize/price-observation->template-entity
   :log-prefix "[expenses] Syncing price observations to template:"})

