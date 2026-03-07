(ns app.admin.frontend.pages.backlog
  "Canonical admin backlog page using generic entity machinery."
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [uix.core :refer [$ defui]]))

(defui admin-backlog-page
  "Admin backlog page"
  []
  ($ generic-admin-entity-page
    {:children :backlog}))
