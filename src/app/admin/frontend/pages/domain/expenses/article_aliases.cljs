(ns app.admin.frontend.pages.domain.expenses.article-aliases
  "Admin Article Aliases page.

  Renders an admin-native list backed by the expenses admin API."
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.components.related-records-wizard :as rr-wizard]
    [app.template.frontend.components.dropdown.action :as dropdown]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.list.cells :as list-cells]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]

    ;; Side-effect requires
    app.domain.frontend.expenses.admin.adapters.admin-crud
    app.domain.frontend.expenses.admin.adapters.specs
    app.domain.frontend.expenses.admin.adapters.sync
    app.domain.frontend.expenses.events.article-aliases
    app.domain.frontend.expenses.subs.article-aliases))

(defn- show-related-records-actions
  [article-alias]
  [{:group-title "Related"
    :items [{:id "show-related-records"
             :icon "\uD83D\uDD17"
             :label "Show related records"
             :on-click (fn [e]
                         (.stopPropagation e)
                         (rf/dispatch [:app.domain.frontend.expenses.events.article-aliases/open-related-records-modal article-alias]))}]}])

(defn- render-article-alias-row-actions
  [article-alias]
  (let [item-id (id-utils/extract-entity-id article-alias)]
    ($ list-cells/action-buttons
      {:item article-alias
       :entity-name :article-aliases
       :show-edit? (:show-edit? article-alias)
       :show-delete? (:show-delete? article-alias)
       :edit-disabled? (:edit-disabled? article-alias)
       :delete-disabled? (:delete-disabled? article-alias)
       :on-edit-click (:on-edit-click article-alias)
       :custom-actions (fn [_]
                         ($ dropdown/action-dropdown
                           {:entity-id item-id
                            :actions (show-related-records-actions article-alias)
                            :position :portal}))})))

(def ^:private article-alias-type-options
  [{:id "expenses"
    :label "Expenses"
    :description "Expenses containing this alias in their items."}
   {:id "receipts"
    :label "Receipts"
    :description "Receipts from expenses containing this alias."}])

(defui admin-article-aliases-page
  "Admin route: /admin/article-aliases"
  []
  (let [entity-name :article-aliases
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:app.domain.frontend.expenses.events.article-aliases/load-list {}]))
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
            "Article Aliases")
          ($ :p {:class "text-sm text-base-content/70 mt-1"}
            "Article aliases from the Expenses domain (admin API)."))

        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Article Aliases"
           :render-actions render-article-alias-row-actions})

        ($ rr-wizard/related-records-wizard
          {:entity-singular "article-alias"
           :entity-key :article-aliases
           :type-options article-alias-type-options
           :entity-name-fn (fn [entity]
                             (or (:raw-label entity)
                               (:raw_label entity)
                               "Selected alias"))})))))
