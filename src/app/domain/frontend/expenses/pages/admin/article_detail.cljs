(ns app.domain.frontend.expenses.pages.admin.article-detail
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.admin.components.detail-views :as detail-views]
    [app.domain.frontend.expenses.events.articles :as articles-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-article-detail-page []
  ($ layout/admin-layout
    (let [current-route (use-subscribe [:current-route])
          article-id (get-in current-route [:path-params :id])
          article (use-subscribe [:expenses/article article-id])]
      ($ :div {:class "p-6 space-y-6"}
        ($ :div {:class "flex items-center justify-between"}
          ($ :div {:class "space-y-1"}
            ($ :div {:class "text-sm breadcrumbs"}
              ($ :ul
                ($ :li ($ :a {:href "/admin/articles"} "Articles"))
                ($ :li (str (or (:canonical-name article) "Article")))))
            ($ :h1 {:class "text-2xl font-bold"} "Article Detail"))
          ($ :div {:class "flex items-center gap-2"}
            ($ :a {:id "btn-back-articles"
                   :href "/admin/articles"
                   :class "ds-btn ds-btn-ghost ds-btn-sm"}
              "Back")
            ($ :button {:id (str "btn-refresh-articles-" article-id)
                        :class "ds-btn ds-btn-outline ds-btn-sm"
                        :on-click #(when article-id
                                     (rf/dispatch [::articles-events/load-detail article-id]))}
              "Refresh")))

        ($ detail-views/article-detail-body {:article-id article-id})))))
