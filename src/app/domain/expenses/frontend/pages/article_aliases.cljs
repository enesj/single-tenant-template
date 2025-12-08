(ns app.domain.expenses.frontend.pages.article-aliases
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [uix.core :refer [$ defui]]))

(defui admin-article-aliases-page []
  ($ generic-admin-entity-page :article-aliases))
