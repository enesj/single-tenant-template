(ns app.domain.expenses.frontend.pages.suppliers
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [uix.core :refer [$ defui]]))

(defui admin-suppliers-page []
  ($ generic-admin-entity-page :suppliers))
