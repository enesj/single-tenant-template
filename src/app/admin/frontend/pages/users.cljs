(ns app.admin.frontend.pages.users
  "Users page UI; wire list/detail actions."
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [uix.core :refer [$ defui]]))

(defui admin-users-page
  "Admin users page using the generic entity system"
  []
  ($ generic-admin-entity-page :users))
