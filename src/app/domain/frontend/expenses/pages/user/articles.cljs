(ns app.domain.frontend.expenses.pages.user.articles
  "Power-user articles list (used for mapping/aliases workflows)."
  (:require
    [app.domain.frontend.expenses.authz :as authz]
    [app.domain.frontend.expenses.components.page-guard :refer [expenses-page-guard]]
    [app.domain.frontend.expenses.components.user-power-forms :refer [user-article-add-form-modal]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.list :refer [list-view]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]
    app.domain.frontend.expenses.subs.user-expenses))

(defn- render-add-form
  [{:keys [on-success on-cancel]}]
  ($ user-article-add-form-modal
    {:on-success on-success
     :on-cancel on-cancel}))

(defui articles-page []
  (let [role (use-subscribe [:expenses/user-role])
        can-manage? (authz/can? role :expenses/articles.manage)
        entity-name :articles
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/fetch-articles]))
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
                 ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "Articles")
                 ($ :p {:class "text-sm text-base-content/70"}
                   "Power-user article catalog (used for mapping and aliases)"))
               ($ :div {:class "flex gap-2 flex-wrap"}
                 ($ button {:id "btn-back-expenses-dashboard-articles"
                            :btn-type :ghost
                            :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                   "Dashboard")
                 ($ button {:id "btn-go-unmapped-items-articles"
                            :btn-type :primary
                            :on-click #(rf/dispatch [:navigate-to "/unmapped-items"])}
                   "Unmapped Items")))))

         ($ :main {:class "w-full px-4 py-6"}
           ($ list-view
             {:entity-name entity-name
              :entity-spec entity-spec
              :title "Articles"
              :form-display :modal
              :allow-add? can-manage?
              :render-add-form render-add-form
              :render-actions (fn [_item] nil)})))})))

