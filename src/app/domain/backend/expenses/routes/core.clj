(ns app.domain.backend.expenses.routes.core
  (:require
    [app.domain.backend.expenses.routes.articles :as articles]
    [app.domain.backend.expenses.routes.expenses :as expenses]
    [app.domain.backend.expenses.routes.article-aliases :as article-aliases]
    [app.domain.backend.expenses.routes.payers :as payers]
    [app.domain.backend.expenses.routes.price-observations :as price-observations]
    [app.domain.backend.expenses.routes.receipts :as receipts]
    [app.domain.backend.expenses.routes.reports :as reports]
    [app.domain.backend.expenses.routes.suppliers :as suppliers]))

(defn routes
  "Top-level router for the Home Expenses domain. Mounted under /admin/api/expenses."
  [db]
  ["/expenses"
   (suppliers/routes db)
   (payers/routes db)
   (receipts/routes db)
   (article-aliases/routes db)
   (price-observations/routes db)
   (expenses/routes db)
   (articles/routes db)
   (reports/routes db)])
