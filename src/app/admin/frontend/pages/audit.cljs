(ns app.admin.frontend.pages.audit
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [uix.core :refer [$ defui]]))

(defui admin-audit-page
  "Admin audit logs page using the generic entity system"
  []
  ($ generic-admin-entity-page :audit-logs))
