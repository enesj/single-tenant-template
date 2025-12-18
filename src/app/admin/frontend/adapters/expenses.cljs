(ns app.admin.frontend.adapters.expenses
  "Adapters and entity specs for the expenses domain (admin).
   Normalizes API responses and syncs them into the template entity store."
  (:require
    [app.admin.frontend.adapters.expenses.admin-crud]
    [app.admin.frontend.adapters.expenses.specs]
    [app.admin.frontend.adapters.expenses.sync]
    [app.admin.frontend.adapters.expenses.ui-state :as ui-state]))

(def init-expenses-adapter! ui-state/init-expenses-adapter!)
(def init-receipts-adapter! ui-state/init-receipts-adapter!)
(def init-suppliers-adapter! ui-state/init-suppliers-adapter!)
(def init-payers-adapter! ui-state/init-payers-adapter!)
(def init-articles-adapter! ui-state/init-articles-adapter!)
(def init-article-aliases-adapter! ui-state/init-article-aliases-adapter!)
(def init-price-observations-adapter! ui-state/init-price-observations-adapter!)

