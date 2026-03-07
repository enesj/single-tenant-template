(ns app.admin.frontend.pages.domain.expenses.subcategories
  "Admin Subcategories page.

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
    app.domain.frontend.expenses.events.categories
    app.domain.frontend.expenses.events.subcategories
    app.domain.frontend.expenses.subs.subcategories))

(defn- show-related-records-actions
  [subcategory]
  [{:group-title "Related"
    :items [{:id "show-related-records"
             :icon "\uD83D\uDD17"
             :label "Show related records"
             :on-click (fn [e]
                         (.stopPropagation e)
                         (rf/dispatch [:app.domain.frontend.expenses.events.subcategories/open-related-records-modal subcategory]))}]}])

(defn- render-subcategory-row-actions
  [subcategory]
  (let [item-id (id-utils/extract-entity-id subcategory)]
    ($ list-cells/action-buttons
      {:item subcategory
       :entity-name :subcategories
       :show-edit? (:show-edit? subcategory)
       :show-delete? (:show-delete? subcategory)
       :edit-disabled? (:edit-disabled? subcategory)
       :delete-disabled? (:delete-disabled? subcategory)
       :on-edit-click (:on-edit-click subcategory)
       :custom-actions (fn [_]
                         ($ dropdown/action-dropdown
                           {:entity-id item-id
                            :actions (show-related-records-actions subcategory)
                            :position :portal}))})))

(def ^:private subcategory-type-options
  [{:id "articles"
    :label "Articles"
    :description "Articles in this subcategory."}])

(defui admin-subcategories-page
  "Admin route: /admin/subcategories"
  []
  (let [entity-name :subcategories
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        refresh-list (use-callback
                       (fn []
                         ;; Categories: reference data for category_id FK select (fetch-mode, no pagination state)
                         (rf/dispatch [:app.domain.frontend.expenses.events.categories/load-list
                                       {:fetch-limit 200 :fetch-offset 0}])
                         (rf/dispatch [:app.domain.frontend.expenses.events.subcategories/load-list
                                       {}]))
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
            "Subcategories")
          ($ :p {:class "text-sm text-base-content/70 mt-1"}
            "Subcategories from the Expenses domain (admin API)."))

        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Subcategories"
           :render-actions render-subcategory-row-actions})

        ($ rr-wizard/related-records-wizard
          {:entity-singular "subcategory"
           :entity-key :subcategories
           :type-options subcategory-type-options
           :entity-name-fn (fn [entity]
                             (or (:name entity)
                               "Selected subcategory"))})))))
