(ns app.domain.frontend.expenses.pages.articles
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [uix.core :refer [$ defui]]))

(defui admin-articles-page []
  ($ generic-admin-entity-page :articles))
