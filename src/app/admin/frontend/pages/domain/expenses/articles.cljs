(ns app.admin.frontend.pages.domain.expenses.articles
  "Admin Articles page.

   Renders an admin-native list backed by the expenses admin API (no iframe), so
   the admin shell stays visible and we don't depend on a user session/capability."
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.components.article-related-records-wizard :as related-records-wizard]
    [app.template.frontend.components.dropdown.action :as dropdown]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.list.cells :as list-cells]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]

    ;; Side-effect requires: register sync handlers, entity spec fallbacks, CRUD bridges,
    ;; and list-loading events.
    app.domain.frontend.expenses.admin.adapters.admin-crud
    app.domain.frontend.expenses.admin.adapters.specs
    app.domain.frontend.expenses.admin.adapters.sync
    [app.domain.frontend.expenses.events.articles :as articles-events]
    [app.template.frontend.events.list.ui-state :as ui-state]))

(defn- show-related-records-actions
  [article]
  [{:group-title "Related"
    :items [{:id "show-related-records"
             :icon "🔗"
             :label "Show related records"
             :on-click (fn [e]
                         (.stopPropagation e)
                         (rf/dispatch [::articles-events/open-related-records-modal article]))}]}])

(defn- render-article-row-actions
  [article]
  (let [item-id (id-utils/extract-entity-id article)]
    ($ list-cells/action-buttons
      {:item article
       :entity-name :articles
       :show-edit? (:show-edit? article)
       :show-delete? (:show-delete? article)
       :edit-disabled? (:edit-disabled? article)
       :delete-disabled? (:delete-disabled? article)
       :on-edit-click (:on-edit-click article)
       :custom-actions (fn [_]
                         ($ dropdown/action-dropdown
                           {:entity-id item-id
                            :actions (show-related-records-actions article)
                            :position :portal}))})))

(defui admin-articles-page
  "Admin route: /admin/articles"
  []
  (let [entity-name :articles
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        refresh-list (use-callback
                       (fn []
                         ;; Server-side pagination: set mode, register refresh event, load page 1
                         (rf/dispatch-sync [::ui-state/set-pagination-mode entity-name :server])
                         (rf/dispatch-sync [::ui-state/set-refresh-event entity-name
                                            [::articles-events/load-list]])
                         (rf/dispatch [::articles-events/load-list {:page 1}]))
                       [])]
    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    ($ layout/admin-layout
      ($ :div {:class "p-6 min-h-screen"}
        ($ :div {:class "mb-6"}
          ($ :h1 {:class "text-2xl font-semibold text-base-content"}
            "Articles")
          ($ :p {:class "text-sm text-base-content/70 mt-1"}
            "Articles from the Expenses domain (admin API)."))

        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Articles"
           :render-actions render-article-row-actions})

        ($ related-records-wizard/article-related-records-wizard)))))
