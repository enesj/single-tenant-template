(ns app.admin.frontend.pages.users
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [uix.core :refer [$ defui]]))

(defui admin-users-page
  "Admin users page using the generic entity system"
  []
  ($ generic-admin-entity-page :users))
