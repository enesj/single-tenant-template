(ns app.domain.backend.expenses.routes.middleware
  "Middleware for blocking admin access to tenant-scoped resources
   unless the admin has an active impersonation grant."
  (:require
    [app.template.backend.routes.admin.utils :as utils]))

(defn wrap-require-impersonation
  "Blocks admin access to tenant data unless impersonation is active.
   Returns 403 when `:impersonation` is absent from the request."
  [handler]
  (fn [request]
    (if (:impersonation request)
      (handler request)
      (utils/error-response
        "Access to tenant data requires an active impersonation grant"
        :status 403))))
