(ns app.admin.frontend.pages.login-events
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [uix.core :refer [$ defui]]))

(defui admin-login-events-page
  "Admin login events page using the generic entity system"
  []
  ($ generic-admin-entity-page :login-events))
