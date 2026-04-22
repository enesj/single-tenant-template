(ns app.domain.backend.expenses.handlers.user-dashboard
  "Handler for the workspace dashboard API endpoint."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.dashboard :as dashboard]
    [taoensso.timbre :as log]))

(defn dashboard-handler
  "Handler factory for GET /expenses/dashboard.
   Returns all dashboard widget data in a single response.
  Admin-only fields (unmapped-alias-count) are only
   included when the user's role is admin or owner."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/expenses-read-roles "Role assignment required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              role (h/get-user-role request)
              power-user? (contains? #{"admin" "owner"} role)]
          (try
            (let [data (dashboard/get-dashboard-data db tenant-id power-user?)]
              (h/json-response {:data data}))
            (catch Exception e
              (log/error e "Error loading dashboard data"
                {:tenant-id tenant-id :role role})
              (h/json-response {:error "Failed to load dashboard"} 500)))))
      (h/unauthorized-response))))
