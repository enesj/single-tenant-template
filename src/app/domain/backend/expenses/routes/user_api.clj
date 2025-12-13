(ns app.domain.backend.expenses.routes.user-api
  "User-facing (non-admin) expenses API routes.

   Mounted by the template API under /api/v1.
   Path prefix for this router is /expenses."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses :as user-expenses-handlers]))

(defn routes
  "User expense routes (requires authenticated user).

  `wrap-user-authentication` is injected by the template to avoid domain -> template coupling."
  [db wrap-user-authentication]
  ["/expenses"
   {:middleware [wrap-user-authentication]}
   ;; Dashboard/summary endpoints
   ["/summary" {:get {:handler (user-expenses-handlers/expense-summary-handler db)}}]
   ["/by-month" {:get {:handler (user-expenses-handlers/spending-by-month-handler db)}}]
   ["/by-supplier" {:get {:handler (user-expenses-handlers/spending-by-supplier-handler db)}}]
   ;; Reference data endpoints (suppliers, payers)
   ["/suppliers" {:get {:handler (user-expenses-handlers/list-suppliers-handler db)}}]
   ["/payers" {:get {:handler (user-expenses-handlers/list-payers-handler db)}}]
   ;; CRUD endpoints
   ["" {:get {:handler (user-expenses-handlers/list-expenses-handler db)}
         :post {:handler (user-expenses-handlers/create-expense-handler db)}}]
   ["/:id" {:get {:handler (user-expenses-handlers/get-expense-handler db)}
            :put {:handler (user-expenses-handlers/update-expense-handler db)}
            :delete {:handler (user-expenses-handlers/delete-expense-handler db)}}]])