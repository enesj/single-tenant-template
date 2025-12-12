(ns app.domain.frontend.expenses.pages.admin.payers
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [uix.core :refer [$ defui]]))

(defui admin-payers-page []
  ($ generic-admin-entity-page :payers))
