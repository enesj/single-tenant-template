(ns app.admin.frontend.pages.domain.expenses.suppliers
  "Admin Suppliers page.

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

    ;; Side-effect requires: register sync handlers, entity spec fallbacks, CRUD bridges,
    ;; and list-loading events.
    app.domain.frontend.expenses.admin.adapters.admin-crud
    app.domain.frontend.expenses.admin.adapters.specs
    app.domain.frontend.expenses.admin.adapters.sync
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    app.domain.frontend.expenses.subs.suppliers
    [app.template.frontend.events.list.ui-state :as ui-state]))

(defn- show-related-records-actions
  [supplier]
  [{:group-title "Related"
    :items [{:id "show-related-records"
             :icon "\uD83D\uDD17"
             :label "Show related records"
             :on-click (fn [e]
                         (.stopPropagation e)
                         (rf/dispatch [:app.domain.frontend.expenses.events.suppliers/open-related-records-modal supplier]))}]}])

(defn- render-supplier-row-actions
  [supplier]
  (let [item-id (id-utils/extract-entity-id supplier)]
    ($ list-cells/action-buttons
      {:item supplier
       :entity-name :suppliers
       :show-edit? (:show-edit? supplier)
       :show-delete? (:show-delete? supplier)
       :edit-disabled? (:edit-disabled? supplier)
       :delete-disabled? (:delete-disabled? supplier)
       :on-edit-click (:on-edit-click supplier)
       :custom-actions (fn [_]
                         ($ dropdown/action-dropdown
                           {:entity-id item-id
                            :actions (show-related-records-actions supplier)
                            :position :portal}))})))

(def ^:private supplier-type-options
  [{:id "expenses"
    :label "Expenses"
    :description "Expenses from this supplier."}
   {:id "receipts"
    :label "Receipts"
    :description "Receipts connected through expenses."}
   {:id "articles"
    :label "Articles"
    :description "Articles associated via aliases or expense items."}
   {:id "stores"
    :label "Stores"
    :description "Store locations owned by this supplier."}])

(defui admin-suppliers-page
  "Admin route: /admin/suppliers"
  []
  (let [entity-name :suppliers
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch-sync [::ui-state/set-pagination-mode entity-name :server])
                         (rf/dispatch-sync [::ui-state/set-refresh-event entity-name
                                            [::suppliers-events/load-list]])
                         (rf/dispatch [::suppliers-events/load-list {:page 1}]))
                       [])]
    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    ($ layout/admin-layout
      ($ :div {:class "p-6 min-h-screen"}
        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Suppliers"
           :render-actions render-supplier-row-actions})

        ($ rr-wizard/related-records-wizard
          {:entity-singular "supplier"
           :entity-key :suppliers
           :type-options supplier-type-options
           :entity-name-fn (fn [entity]
                             (or (:display-name entity)
                               (:display_name entity)
                               "Selected supplier"))})))))
