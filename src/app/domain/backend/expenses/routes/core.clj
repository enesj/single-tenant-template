(ns app.domain.backend.expenses.routes.core
  "Expenses backend route assembly; add new expense endpoints here."
  (:require
    [app.domain.backend.expenses.handlers.receipt-upload :as receipt-upload]
    [app.domain.backend.expenses.routes.articles :as articles]
    [app.domain.backend.expenses.routes.article-aliases :as article-aliases]
    [app.domain.backend.expenses.routes.expense-items :as expense-items]
    [app.domain.backend.expenses.routes.expenses :as expenses]
    [app.domain.backend.expenses.routes.payers :as payers]
    [app.domain.backend.expenses.routes.price-observations :as price-observations]
    [app.domain.backend.expenses.routes.receipts :as receipts]
    [app.domain.backend.expenses.routes.reports :as reports]
    [app.domain.backend.expenses.routes.suppliers :as suppliers]))

(defn routes
  "Top-level router for the Home Expenses domain. Mounted under /admin/api/expenses.
  
  app-config is optional and is passed to routes that need it (e.g., receipts for OCR)."
  [db & [app-config]]
  ["/expenses"
   ["/upload" {:post {:handler (receipt-upload/admin-upload-handler db)}}]
   (suppliers/routes db)
   (payers/routes db)
   (receipts/routes db app-config)
   (article-aliases/routes db)
   (price-observations/routes db)
   (expenses/routes db)
   (expense-items/routes db)
   (articles/routes db)
   (reports/routes db)])
