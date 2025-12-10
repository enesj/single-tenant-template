(ns app.backend.routes.admin-api
  "Refactored admin API routes - now composed from focused namespaces"
  (:require
   [app.backend.middleware.admin :as admin-middleware] ;; Focused admin route namespaces
   [app.backend.routes.admin.admins :as admin-admins]
   [app.backend.routes.admin.audit :as admin-audit]
   [app.backend.routes.admin.auth :as admin-auth]
   [app.backend.routes.admin.dashboard :as admin-dashboard]
   [app.backend.routes.admin.entities :as admin-entities]
   [app.backend.routes.admin.login-events :as admin-login-events]
   [app.backend.routes.admin.password :as admin-password]
   [app.backend.routes.admin.settings :as admin-settings]
   [app.backend.routes.admin.user-bulk :as admin-user-bulk]
   [app.backend.routes.admin.user-operations :as admin-user-ops]
   [app.backend.routes.admin.users :as admin-users]
   [app.backend.routes.admin.utils :as admin-utils]
   [app.domain.expenses.routes.core :as expenses-routes]))


(defn admin-api-routes
  "Admin API routes"
  [db service-container]
  (let [email-service (get service-container :email-service)
        base-url (get-in service-container [:config :base-url] "http://localhost:8085")]
    ["/admin/api"
     ;; Shared middleware for all admin routes
     {:middleware [admin-utils/wrap-json-body-parsing]}

     ;; 🔓 Public routes (no authentication required)
     (admin-auth/routes db)

     ;; 🔓 Public password routes (no authentication required)
     ["/auth" (admin-password/public-routes db email-service base-url)]

     ;; 🔒 Protected routes (require authentication)
     ["" {:middleware [#(admin-middleware/wrap-admin-authentication % db)]}

      ;; Dashboard routes
      ["/dashboard" (admin-dashboard/routes db)]

      ;; Entity management routes
      ["/entities" (admin-entities/routes db nil)]

      ;; Audit logs
      ["/audit" (admin-audit/routes db)]

      ;; Login events
      ["/login-events" (admin-login-events/routes db)]

      ;; Settings (view-options.edn management)
      (admin-settings/routes db)

      ;; Protected password routes
      ["/auth" (admin-password/protected-routes db email-service base-url)]

      ;; Home Expenses domain
      (expenses-routes/routes db)

      ;; Admin management (owner operations)
      (admin-admins/routes db)

      ;; User operations
      ["/users"
       (admin-users/routes db)
       (admin-user-ops/routes db service-container)
       ["/actions" (admin-user-bulk/routes db)]]]]))
