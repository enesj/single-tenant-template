(ns app.domain.frontend.expenses.pages.expense-list
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [uix.core :refer [$ defui]]))

(defui admin-expense-list-page []
  ($ generic-admin-entity-page :expenses))
