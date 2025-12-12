(ns app.domain.frontend.expenses.pages.admin.receipts
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [uix.core :refer [$ defui]]))

(defui admin-receipts-page []
  ($ generic-admin-entity-page :receipts))
