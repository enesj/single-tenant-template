(ns app.domain.backend.expenses.routes.middleware
  "Middleware for private tenant-scoped admin resources that should remain unavailable
   from the global admin surface."
  (:require
    [app.template.backend.routes.admin.utils :as utils]))

(defn wrap-block-private-admin-resource
  "Blocks global admin access to tenant-scoped resources that expose private linkage.
   Receipt and expense review routes expose dedicated privacy-scrubbed admin views instead."
  [_handler]
  (fn [_request]
    (utils/error-response
      "Admin access to this tenant-scoped resource is disabled"
      :status 403)))
