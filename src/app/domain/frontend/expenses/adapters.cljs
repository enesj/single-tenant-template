(ns app.domain.frontend.expenses.adapters
  "Domain adapters for expenses - re-exports from domain admin adapters.
   
   This namespace provides a stable interface for the domain registry
   to access adapter functions."
  (:require
    [app.domain.frontend.expenses.admin.adapters :as domain-adapters]))

;; Re-export adapter init functions from domain adapters
(def init-expenses-adapter! domain-adapters/init-expenses-adapter!)
(def init-receipts-adapter! domain-adapters/init-receipts-adapter!)
(def init-suppliers-adapter! domain-adapters/init-suppliers-adapter!)
(def init-payers-adapter! domain-adapters/init-payers-adapter!)
(def init-articles-adapter! domain-adapters/init-articles-adapter!)
(def init-article-aliases-adapter! domain-adapters/init-article-aliases-adapter!)
(def init-price-observations-adapter! domain-adapters/init-price-observations-adapter!)
