(ns app.admin.frontend.pages.admins
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [uix.core :refer [$ defui]]))

(defui admin-admins-page
  "Admin management page using the generic entity system"
  []
  ($ generic-admin-entity-page :admins))
