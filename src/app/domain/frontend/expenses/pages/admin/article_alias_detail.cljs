(ns app.domain.frontend.expenses.pages.admin.article-alias-detail
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.admin.components.detail-views :as detail-views]
    [app.domain.frontend.expenses.events.article-aliases :as aliases-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-article-alias-detail-page []
  ($ layout/admin-layout
    (let [current-route (use-subscribe [:current-route])
          alias-id (get-in current-route [:path-params :id])
          alias (use-subscribe [:expenses/article-alias alias-id])]
      ($ :div {:class "p-6 space-y-6"}
        ($ :div {:class "flex items-center justify-between"}
          ($ :div {:class "space-y-1"}
            ($ :div {:class "text-sm breadcrumbs"}
              ($ :ul
                ($ :li ($ :a {:href "/admin/article-aliases"} "Article Aliases"))
                ($ :li (str (or (:raw-label-normalized alias) "Alias")))))
            ($ :h1 {:class "text-2xl font-bold"} "Article Alias Detail"))
          ($ :div {:class "flex items-center gap-2"}
            ($ :a {:id "btn-back-article-aliases"
                   :href "/admin/article-aliases"
                   :class "ds-btn ds-btn-ghost ds-btn-sm"}
              "Back")
            ($ :button {:id (str "btn-refresh-article-aliases-" alias-id)
                        :class "ds-btn ds-btn-outline ds-btn-sm"
                        :on-click #(when alias-id
                                     (rf/dispatch [::aliases-events/load-detail alias-id]))}
              "Refresh")))

        ($ detail-views/article-alias-detail-body {:alias-id alias-id})))))
