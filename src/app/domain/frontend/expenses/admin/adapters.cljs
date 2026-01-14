(ns app.domain.frontend.expenses.admin.adapters
  "Aggregator for expenses domain admin adapters.
   Loads all adapter modules and re-exports init functions."
  (:require
    ;; Load adapter modules for side effects (registration)
    app.domain.frontend.expenses.admin.adapters.admin-crud
    app.domain.frontend.expenses.admin.adapters.specs
    app.domain.frontend.expenses.admin.adapters.sync
    app.domain.frontend.expenses.admin.form-components
    [app.domain.frontend.expenses.admin.adapters.ui-state :as ui-state]))

  ;; Generic init helpers (preferred)
  (def init-entity-adapter! ui-state/init-entity-adapter!)
  (def entity-init-fns ui-state/entity-init-fns)
  (def init-all-adapters! ui-state/init-all-adapters!)

;; Re-export adapter init functions
(def init-expenses-adapter! ui-state/init-expenses-adapter!)
(def init-receipts-adapter! ui-state/init-receipts-adapter!)
(def init-suppliers-adapter! ui-state/init-suppliers-adapter!)
(def init-payers-adapter! ui-state/init-payers-adapter!)
(def init-articles-adapter! ui-state/init-articles-adapter!)
(def init-article-aliases-adapter! ui-state/init-article-aliases-adapter!)
(def init-price-observations-adapter! ui-state/init-price-observations-adapter!)
(def init-expense-items-adapter! ui-state/init-expense-items-adapter!)
