(ns app.template.backend.routes.admin-api
  "Compose /admin/api routes and middleware; add admin endpoints via focused route namespaces."
  (:require
    [app.template.backend.email.service :as email-svc]
    [app.template.backend.middleware.admin :as admin-middleware]
    [app.template.backend.routes.admin.admin-invitations :as admin-invitations]
    [app.template.backend.routes.admin.admins :as admin-admins]
    [app.template.backend.routes.admin.tenants :as admin-tenants]
    [app.template.backend.routes.admin.audit :as admin-audit]
    [app.template.backend.routes.admin.auth :as admin-auth]
    [app.template.backend.routes.admin.dashboard :as admin-dashboard]
    [app.template.backend.routes.admin.entities :as admin-entities]
    [app.template.backend.routes.admin.impersonation :as admin-impersonation]
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
        base-url (email-svc/create-base-url (:config service-container))
        password-db (or (get service-container :db-adapter) db)
        ;; Collect domain routes from registry
        domain-routes (domain-registry/all-admin-api-routes db service-container)]
    ["/admin/api"
     ;; Shared middleware for all admin routes
     {:middleware [admin-utils/wrap-json-body-parsing]}

     ;; 🔓 Public routes (no authentication required)
     (admin-auth/routes db)

     ;; 🔓 Public password routes (no authentication required)
     ["/auth" (admin-password/public-routes password-db email-service base-url)]

     ;; 🔓 Public invitation routes (accept/validate — invitee has no account yet)
     (admin-invitations/public-routes db)

     ;; 🔒 Protected routes (require authentication)
     ["" {:middleware [#(admin-middleware/wrap-admin-authentication % db)]}

      ;; Dashboard routes
      ["/dashboard" (admin-dashboard/routes db)]

      ;; Entity management routes
      (admin-entities/routes db nil)

      ;; Audit logs
      ["/audit" (admin-audit/routes db)]

      ;; Login events
      ["/login-events" (admin-login-events/routes db)]

      ;; Settings (view-options.edn management)
      (admin-settings/routes db)

      ;; Protected password routes
      ["/auth" (admin-password/protected-routes password-db email-service base-url)]

      ;; Impersonation management
      (admin-impersonation/routes db)

      ;; Domain routes (from registry)
      ;; Each domain provides routes under its own path prefix (e.g., /expenses)
      (into [] (filter some? domain-routes))

      ;; Tenant management (superpower CRUD)
      (admin-tenants/routes db)

      ;; Admin management (owner operations)
      (admin-admins/routes db)

      ;; Admin invitations (owner operations)
      (admin-invitations/protected-routes db email-service base-url)

      ;; User operations
      ["/users"
       (admin-users/routes db)
       (admin-user-ops/routes db service-container)
       ["/actions" (admin-user-bulk/routes db)]]]]))
