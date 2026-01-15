(ns app.template.backend.routes.admin-api
  "Compose /admin/api routes and middleware; add admin endpoints via focused route namespaces."
  (:require
    [app.template.backend.middleware.admin :as admin-middleware]
    [app.template.backend.routes.admin.admins :as admin-admins]
    [app.template.backend.routes.admin.audit :as admin-audit]
    [app.template.backend.routes.admin.auth :as admin-auth]
    [app.template.backend.routes.admin.dashboard :as admin-dashboard]
    [app.template.backend.routes.admin.entities :as admin-entities]
    [app.template.backend.routes.admin.login-events :as admin-login-events]
    [app.template.backend.routes.admin.password :as admin-password]
    [app.template.backend.routes.admin.settings :as admin-settings]
    [app.template.backend.routes.admin.user-bulk :as admin-user-bulk]
    [app.template.backend.routes.admin.user-operations :as admin-user-ops]
    [app.template.backend.routes.admin.users :as admin-users]
    [app.template.backend.routes.admin.utils :as admin-utils]
    [app.domain.backend.registry :as domain-registry]))

(defn admin-api-routes
  "Admin API routes"
  [db service-container]
  (let [email-service (get service-container :email-service)
        base-url (get-in service-container [:config :base-url] "http://localhost:8085")
        ;; Collect domain routes from registry
        domain-routes (domain-registry/all-admin-api-routes db service-container)]
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

      ;; Domain routes (from registry)
      ;; Each domain provides routes under its own path prefix (e.g., /expenses)
      (into [] (filter some? domain-routes))

      ;; Admin management (owner operations)
      (admin-admins/routes db)

      ;; User operations
      ["/users"
       (admin-users/routes db)
       (admin-user-ops/routes db service-container)
       ["/actions" (admin-user-bulk/routes db)]]]]))
