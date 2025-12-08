(ns app.domain.expenses.frontend.pages.price-observations
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [uix.core :refer [$ defui]]))

(defui admin-price-observations-page []
  ($ generic-admin-entity-page :price-observations))
