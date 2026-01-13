(ns app.domain.frontend.expenses.pages.user.article-aliases
  "Power-user view of article aliases (read-only list for now)."
  (:require
    [app.domain.frontend.expenses.components.page-guard :refer [expenses-page-guard]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.list :refer [list-view]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]
    app.domain.frontend.expenses.subs.user-expenses))

(defui article-aliases-page []
  (let [entity-name :article-aliases
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/fetch-article-aliases]))
                       [])]
    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    ($ expenses-page-guard
      {:capability :expenses/articles.manage
       :children
       ($ :div {:class "min-h-screen bg-base-100"}
         ($ :header {:class "bg-white border-b border-base-200"}
           ($ :div {:class "w-full px-4 py-4 sm:py-6"}
             ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
               ($ :div
                 ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "Article Aliases")
                 ($ :p {:class "text-sm text-base-content/70"}
                   "Power-user alias catalog (mapping is managed via Unmapped Items)"))
               ($ :div {:class "flex gap-2 flex-wrap"}
                 ($ button {:id "btn-back-expenses-dashboard-article-aliases"
                            :btn-type :ghost
                            :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                   "Dashboard")
                 ($ button {:id "btn-go-unmapped-items-article-aliases"
                            :btn-type :primary
                            :on-click #(rf/dispatch [:navigate-to "/unmapped-items"])}
                   "Unmapped Items")))))

         ($ :main {:class "w-full px-4 py-6"}
           ($ list-view
             {:entity-name entity-name
              :entity-spec entity-spec
              :title "Article Aliases"
              :form-display :modal
              :allow-add? false
              :render-actions (fn [_item] nil)})))})))

