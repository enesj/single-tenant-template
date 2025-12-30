(ns app.domain.backend.expenses.routes.user-api
  "User-facing (non-admin) expenses API routes.

   Mounted by the template API under /api/v1.
   Path prefix for this router is /expenses."
  (:require
    [app.domain.backend.expenses.handlers.receipt-upload :as receipt-upload]
    [app.domain.backend.expenses.handlers.user-expenses :as user-expenses-handlers]
    [app.domain.backend.expenses.handlers.user-receipts :as user-receipts]))

(defn routes
  "User expense routes (requires authenticated user).

  `wrap-user-authentication` is injected by the template to avoid domain -> template coupling.
  `app-config` is optional and used for OCR routes."
  [db wrap-user-authentication & [app-config]]
  ["/expenses"
   {:middleware [wrap-user-authentication]}

   ;; Dashboard/summary endpoints
   ["/summary" {:get {:handler (user-expenses-handlers/expense-summary-handler db)}}]
   ["/by-month" {:get {:handler (user-expenses-handlers/spending-by-month-handler db)}}]
   ["/by-supplier" {:get {:handler (user-expenses-handlers/spending-by-supplier-handler db)}}]

   ;; Reference data endpoints (suppliers, payers)
   ["/suppliers"
    {:get {:handler (user-expenses-handlers/list-suppliers-handler db)}
     :post {:handler (user-expenses-handlers/create-supplier-handler db)}}]

   ["/suppliers/:id"
    {:put {:handler (user-expenses-handlers/update-supplier-handler db)}
     :delete {:handler (user-expenses-handlers/delete-supplier-handler db)}}]

   ["/payers"
    {:get {:handler (user-expenses-handlers/list-payers-handler db)}
     :post {:handler (user-expenses-handlers/create-payer-handler db)}}]

   ["/payers/:id"
    {:put {:handler (user-expenses-handlers/update-payer-handler db)}
     :delete {:handler (user-expenses-handlers/delete-payer-handler db)}}]

   ;; Receipt upload (creates a receipts row)
   ["/upload" {:post {:handler (receipt-upload/user-upload-handler db)}}]

   ;; Receipts inbox (review + approve + OCR)
   ["/receipts" {:get {:handler (user-receipts/list-receipts-handler db)}}]
   ["/receipts/ocr" {:post {:handler (user-receipts/ocr-batch-receipts-handler db app-config)}}]
   ["/receipts/:id"
    {:get {:handler (user-receipts/get-receipt-handler db)}
     :delete {:handler (user-receipts/delete-receipt-handler db)}}]
   ["/receipts/:id/approve" {:post {:handler (user-receipts/approve-receipt-handler db)}}]
   ["/receipts/:id/ocr" {:post {:handler (user-receipts/ocr-single-receipt-handler db app-config)}}]

   ;; Expenses CRUD
   ["" {:get {:handler (user-expenses-handlers/list-expenses-handler db)}
        :post {:handler (user-expenses-handlers/create-expense-handler db)}}]
   ["/:id" {:get {:handler (user-expenses-handlers/get-expense-handler db)}
            :put {:handler (user-expenses-handlers/update-expense-handler db)}
            :delete {:handler (user-expenses-handlers/delete-expense-handler db)}}]])
