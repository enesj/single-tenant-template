(ns app.domain.expenses.routes.core
  (:require
    [app.domain.expenses.routes.articles :as articles]
    [app.domain.expenses.routes.expenses :as expenses]
    [app.domain.expenses.routes.article-aliases :as article-aliases]
    [app.domain.expenses.routes.payers :as payers]
    [app.domain.expenses.routes.price-observations :as price-observations]
    [app.domain.expenses.routes.receipts :as receipts]
    [app.domain.expenses.routes.reports :as reports]
    [app.domain.expenses.routes.suppliers :as suppliers]))

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
