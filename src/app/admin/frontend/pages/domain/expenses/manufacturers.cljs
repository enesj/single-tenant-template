(ns app.admin.frontend.pages.domain.expenses.manufacturers
  "Admin Manufacturers page.

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
    [app.domain.frontend.expenses.events.manufacturers :as manufacturers-events]
    app.domain.frontend.expenses.subs.manufacturers
    [app.template.frontend.events.list.ui-state :as ui-state]))

(defn- show-related-records-actions
  [manufacturer]
  [{:group-title "Related"
    :items [{:id "show-related-records"
             :icon "\uD83D\uDD17"
             :label "Show related records"
             :on-click (fn [e]
                         (.stopPropagation e)
                         (rf/dispatch [:app.domain.frontend.expenses.events.manufacturers/open-related-records-modal manufacturer]))}]}])

(defn- render-manufacturer-row-actions
  [manufacturer]
  (let [item-id (id-utils/extract-entity-id manufacturer)]
    ($ list-cells/action-buttons
      {:item manufacturer
       :entity-name :manufacturers
       :show-edit? (:show-edit? manufacturer)
       :show-delete? (:show-delete? manufacturer)
       :edit-disabled? (:edit-disabled? manufacturer)
       :delete-disabled? (:delete-disabled? manufacturer)
       :on-edit-click (:on-edit-click manufacturer)
       :custom-actions (fn [_]
                         ($ dropdown/action-dropdown
                           {:entity-id item-id
                            :actions (show-related-records-actions manufacturer)
                            :position :portal}))})))

(def ^:private manufacturer-type-options
  [{:id "articles"
    :label "Articles"
    :description "Articles from this manufacturer."}])

(defui admin-manufacturers-page
  "Admin route: /admin/manufacturers"
  []
  (let [entity-name :manufacturers
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch-sync [::ui-state/set-pagination-mode entity-name :server])
                         (rf/dispatch-sync [::ui-state/set-refresh-event entity-name
                                            [::manufacturers-events/load-list]])
                         (rf/dispatch [::manufacturers-events/load-list {:page 1}]))
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
            "Manufacturers")
          ($ :p {:class "text-sm text-base-content/70 mt-1"}
            "Manufacturers from the Expenses domain (admin API)."))

        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Manufacturers"
           :render-actions render-manufacturer-row-actions})

        ($ rr-wizard/related-records-wizard
          {:entity-singular "manufacturer"
           :entity-key :manufacturers
           :type-options manufacturer-type-options
           :entity-name-fn (fn [entity]
                             (or (:display-name entity)
                               (:display_name entity)
                               "Selected manufacturer"))})))))
