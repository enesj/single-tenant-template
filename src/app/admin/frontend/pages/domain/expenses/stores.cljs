(ns app.admin.frontend.pages.domain.expenses.stores
  "Admin Stores page.

  Renders an admin-native list backed by the expenses admin API."
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.components.related-records-wizard :as rr-wizard]
    [app.template.frontend.components.dropdown.action :as dropdown]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.list.cells :as list-cells]
    [app.template.frontend.events.list.ui-state :as ui-state]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]

    ;; Side-effect requires
    app.domain.frontend.expenses.admin.adapters.admin-crud
    app.domain.frontend.expenses.admin.adapters.specs
    app.domain.frontend.expenses.admin.adapters.sync
    app.domain.frontend.expenses.events.cities
    [app.domain.frontend.expenses.events.stores :as stores-events]
    app.domain.frontend.expenses.subs.stores))

(defn- show-related-records-actions
  [store]
  [{:group-title "Related"
    :items [{:id "show-related-records"
             :icon "\uD83D\uDD17"
             :label "Show related records"
             :on-click (fn [e]
                         (.stopPropagation e)
                         (rf/dispatch [:app.domain.frontend.expenses.events.stores/open-related-records-modal store]))}]}])

(defn- render-store-row-actions
  [store]
  (let [item-id (id-utils/extract-entity-id store)]
    ($ list-cells/action-buttons
      {:item store
       :entity-name :stores
       :show-edit? (:show-edit? store)
       :show-delete? (:show-delete? store)
       :edit-disabled? (:edit-disabled? store)
       :delete-disabled? (:delete-disabled? store)
       :on-edit-click (:on-edit-click store)
       :custom-actions (fn [_]
                         ($ dropdown/action-dropdown
                           {:entity-id item-id
                            :actions (show-related-records-actions store)
                            :position :portal}))})))

(def ^:private store-type-options
  [{:id "expenses"
    :label "Expenses"
    :description "Expenses at this store location."}
   {:id "receipts"
    :label "Receipts"
    :description "Receipts connected through expenses."}
   {:id "articles"
    :label "Articles"
    :description "Articles purchased at this store."}])

(defn dispatch-admin-stores-refresh!
  [dispatch! dispatch-sync!]
  (dispatch-sync! [::ui-state/set-pagination-mode :stores :server])
  (dispatch-sync! [::ui-state/set-refresh-event :stores [::stores-events/load-list]])
  ;; Cities are reference data for filter/select rendering and should stay unpaginated.
  (dispatch! [:app.domain.frontend.expenses.events.cities/load-list
              {:fetch-limit 200 :fetch-offset 0}])
  (dispatch! [::stores-events/load-list {:page 1}]))

(defui admin-stores-page
  "Admin route: /admin/stores"
  []
  (let [entity-name :stores
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-list (use-callback
                       (fn []
                         (dispatch-admin-stores-refresh! rf/dispatch rf/dispatch-sync))
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
            "Stores")
          ($ :p {:class "text-sm text-base-content/70 mt-1"}
            "Stores (locations/branches) from the Expenses domain (admin API)."))

        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Stores"
           :form-display :modal
           :allow-add? false
           :allow-edit? true
           :allow-delete? true
           :disallowed-action-mode :hide
           :render-actions render-store-row-actions})

        ($ rr-wizard/related-records-wizard
          {:entity-singular "store"
           :entity-key :stores
           :type-options store-type-options
           :entity-name-fn (fn [entity]
                             (or (:display-name entity)
                               (:display_name entity)
                               "Selected store"))})))))
