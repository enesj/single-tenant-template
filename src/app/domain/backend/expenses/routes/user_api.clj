(ns app.domain.backend.expenses.routes.user-api
  "User-facing (non-admin) expenses API routes.

   Mounted by the template API under /api/v1.
   Path prefix for this router is /expenses."
  (:require
    [app.domain.backend.expenses.handlers.receipt-upload :as receipt-upload]
    [app.domain.backend.expenses.handlers.user-expenses.supplier-detail :as supplier-detail]
    [app.domain.backend.expenses.handlers.user-articles :as user-articles]
    [app.domain.backend.expenses.handlers.user-price-observations :as user-price-observations]
    [app.domain.backend.expenses.handlers.user-expenses.batch :as user-expenses-batch]
    [app.domain.backend.expenses.handlers.user-expenses.crud :as user-expenses-crud]
    [app.domain.backend.expenses.handlers.user-expenses.expense-items :as user-expenses-expense-items]
    [app.domain.backend.expenses.handlers.user-expenses.reference-data :as user-expenses-reference-data]
    [app.domain.backend.expenses.handlers.user-expenses.summary :as user-expenses-summary]
    [app.domain.backend.expenses.handlers.user-expenses.settings :as settings]
    [app.domain.backend.expenses.handlers.user-receipts :as user-receipts]))

(defn routes
  "User expense routes (requires authenticated user).

  `wrap-user-authentication` is injected by the template to avoid domain -> template coupling.
  `app-config` is optional and used for OCR routes."
  [db wrap-user-authentication & [app-config]]
  ["/expenses"
   {:middleware [wrap-user-authentication]}

   ;; Dashboard/summary endpoints
   ["/summary" {:get {:handler (user-expenses-summary/expense-summary-handler db)}}]
   ["/by-month" {:get {:handler (user-expenses-summary/spending-by-month-handler db)}}]
   ["/by-supplier" {:get {:handler (user-expenses-summary/spending-by-supplier-handler db)}}]

   ;; Settings endpoints (must come before /:id routes)
   ["/settings"
    {:get {:handler (settings/get-settings-handler db)}
     :put {:handler (settings/update-settings-handler db)}}]

   ;; Export endpoint
   ["/export" {:get {:handler (settings/export-expenses-handler db)}}]

   ;; Delete-all endpoint (danger zone, admin/owner only)
   ["/all" {:delete {:handler (settings/delete-all-expenses-handler db)}}]

   ;; Reference data endpoints (suppliers, payers)
   ["/suppliers"
    {:get {:handler (user-expenses-reference-data/list-suppliers-handler db)}
     :post {:handler (user-expenses-reference-data/create-supplier-handler db)}}]

   ["/suppliers/:id"
    {:get {:handler (user-expenses-reference-data/get-supplier-handler db)}
     :put {:handler (user-expenses-reference-data/update-supplier-handler db)}
     :delete {:handler (user-expenses-reference-data/delete-supplier-handler db)}}]

   ;; Supplier detail related lists (used by user Suppliers "View Details" modal)
   ["/article-aliases" {:get {:handler (supplier-detail/list-article-aliases-handler db)}}]

   ["/price-observations"
    ["" {:get {:handler (supplier-detail/list-price-observations-handler db)}}]

    ["/:id"
     {:put {:handler (user-price-observations/update-price-observation-handler db)}
      :delete {:handler (user-price-observations/delete-price-observation-handler db)}}]]

   ["/payers"
    {:get {:handler (user-expenses-reference-data/list-payers-handler db)}
     :post {:handler (user-expenses-reference-data/create-payer-handler db)}}]

   ["/payers/:id"
    {:put {:handler (user-expenses-reference-data/update-payer-handler db)}
     :delete {:handler (user-expenses-reference-data/delete-payer-handler db)}}]

   ;; Payer Types (admin/owner only)
   ["/payer-types"
    {:get {:handler (user-expenses-reference-data/list-payer-types-handler db)}
     :post {:handler (user-expenses-reference-data/create-payer-type-handler db)}}]

   ["/payer-types/:id"
    {:put {:handler (user-expenses-reference-data/update-payer-type-handler db)}
     :delete {:handler (user-expenses-reference-data/delete-payer-type-handler db)}}]

   ;; Expense items (power-user only)
   ["/expense-items" {:get {:handler (user-expenses-expense-items/list-expense-items-handler db)}}]

   ["/expense-items/:id"
    {:put {:handler (user-expenses-expense-items/update-expense-item-handler db)}
     :delete {:handler (user-expenses-expense-items/delete-expense-item-handler db)}}]

   ;; Receipt upload (creates a receipts row)
   ["/upload" {:post {:handler (receipt-upload/user-upload-handler db)}}]

   ;; Receipts inbox (review + approve + OCR)
   ["/receipts" {:get {:handler (user-receipts/list-receipts-handler db)}}]
   ["/receipts/ocr" {:post {:handler (user-receipts/ocr-batch-receipts-handler db app-config)}}]
   ["/receipts/:id/download" {:get {:handler (user-receipts/download-receipt-handler db)}}]
   ["/receipts/:id"
    {:get {:handler (user-receipts/get-receipt-handler db)}
     :delete {:handler (user-receipts/delete-receipt-handler db)}}]
   ["/receipts/:id/approve" {:post {:handler (user-receipts/approve-receipt-handler db)}}]
   ["/receipts/:id/review" {:post {:handler (user-receipts/save-receipt-review-handler db)}}]
   ["/receipts/:id/ocr" {:post {:handler (user-receipts/ocr-single-receipt-handler db app-config)}}]

  ;; Articles + unmapped aliases (role-gated to admin/owner)
   ;; IMPORTANT: Must come before the "/:id" expense route.
   ["/articles"
    ["" {:get {:handler (user-articles/list-articles-handler db)}
         :post {:handler (user-articles/create-article-handler db)}}]

      ["/unmapped-aliases" {:get {:handler (user-articles/list-unmapped-aliases-handler db)}}]

      ["/aliases/:alias-id/map" {:post {:handler (user-articles/map-alias-to-article-handler db)}}]

    ["/:id/aliases" {:post {:handler (user-articles/batch-create-aliases-handler db)}}]

    ["/:id" {:put {:handler (user-articles/update-article-handler db)}
             :delete {:handler (user-articles/delete-article-handler db)}}]]

   ;; Expenses CRUD
   ["" {:get {:handler (user-expenses-crud/list-expenses-handler db)}
        :post {:handler (user-expenses-crud/create-expense-handler db)}}]

   ;; Batch operations
   ["/batch" {:put {:handler (user-expenses-batch/batch-update-expenses-handler db)}}]

   ["/batch-delete" {:post {:handler (user-expenses-batch/batch-delete-expenses-handler db)}}]

   ["/:id" {:get {:handler (user-expenses-crud/get-expense-handler db)}
            :put {:handler (user-expenses-crud/update-expense-handler db)}
            :delete {:handler (user-expenses-crud/delete-expense-handler db)}}]])
