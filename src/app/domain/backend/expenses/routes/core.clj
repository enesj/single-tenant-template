(ns app.domain.backend.expenses.routes.core
  "Expenses backend route assembly; add new expense endpoints here."
  (:require
    [app.domain.backend.expenses.handlers.search :as search]
    [app.domain.backend.expenses.routes.articles :as articles]
    [app.domain.backend.expenses.routes.article-aliases :as article-aliases]
    [app.domain.backend.expenses.routes.categories :as categories]
    [app.domain.backend.expenses.routes.cities :as cities]
    [app.domain.backend.expenses.routes.countries :as countries]
    [app.domain.backend.expenses.routes.duplicates :as duplicates]
    [app.domain.backend.expenses.routes.expense-categories :as expense-categories]
    [app.domain.backend.expenses.routes.expense-items :as expense-items]
    [app.domain.backend.expenses.routes.expenses :as expenses]
    [app.domain.backend.expenses.routes.manufacturers :as manufacturers]
    [app.domain.backend.expenses.routes.payer-types :as payer-types]
    [app.domain.backend.expenses.routes.payers :as payers]

    [app.domain.backend.expenses.routes.receipts :as receipts]
    [app.domain.backend.expenses.routes.reports :as reports]
    [app.domain.backend.expenses.routes.store-aliases :as store-aliases]
    [app.domain.backend.expenses.routes.stores :as stores]
    [app.domain.backend.expenses.routes.subcategories :as subcategories]
    [app.domain.backend.expenses.routes.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.routes.suppliers :as suppliers]))

(defn routes
  "Top-level router for the Home Expenses domain. Mounted under /admin/api/expenses."
  [db & [app-config]]
  ["/expenses"
   ;; Cross-entity admin search (global, no tenant filter)
   ["/search" {:get {:handler (search/admin-search-handler db)}}]

   (suppliers/routes db)
   (stores/routes db)
   (cities/routes db)
   (countries/routes db)
   (manufacturers/routes db)
   (categories/routes db)
   (expense-categories/routes db)
   (subcategories/routes db)
   (payers/routes db)
   (payer-types/routes db)
   (receipts/routes db app-config)
   (article-aliases/routes db)
   (supplier-aliases/routes db)
   (store-aliases/routes db)

   (expenses/routes db)
   (expense-items/routes db)
   (articles/routes db)
   (reports/routes db)
   (duplicates/routes db)])




