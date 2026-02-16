(ns app.domain.frontend.expenses.pages.user.cities
  "Power-user cities list (used for store normalization)."
  (:require
    [app.domain.frontend.expenses.authz :as authz]
    [app.domain.frontend.expenses.components.page-guard :refer [expenses-page-guard]]
    [app.domain.frontend.expenses.components.user-power-forms :refer [user-city-add-form-modal
                                                                      user-city-edit-form-modal]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.events.list.ui-state :as list-ui-state-events]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]
    app.domain.frontend.expenses.subs.user-expenses))

(defn- render-add-form
  [{:keys [on-success on-cancel]}]
  ($ user-city-add-form-modal
    {:on-success on-success
     :on-cancel on-cancel}))

(defn- render-edit-form
  [item {:keys [on-success on-cancel]}]
  ($ user-city-edit-form-modal
    {:item item
     :on-success on-success
     :on-cancel on-cancel}))

(defn- render-actions
  [item]
  (let [city-id (id-utils/extract-entity-id item)
        city-id-str (some-> city-id str)
        on-edit-click (:on-edit-click item)
        show-edit? (not (false? (:show-edit? item)))
        show-delete? (not (false? (:show-delete? item)))
        edit-disabled? (true? (:edit-disabled? item))
        delete-disabled? (true? (:delete-disabled? item))
        item-data (dissoc item :show-edit? :show-delete? :edit-disabled? :delete-disabled? :on-edit-click)]
    ($ :div {:class "flex items-center justify-center gap-2"}
      (when show-edit?
        ($ button
          {:id (str "btn-edit-cities-" city-id-str)
           :btn-type :primary
           :shape "circle"
           :disabled edit-disabled?
           :on-click (fn [e]
                       (.stopPropagation e)
                       (when-not edit-disabled?
                         (when on-edit-click
                           (on-edit-click item-data))))}
          ($ edit-icon)))

      (when show-delete?
        ($ button
          {:id (str "btn-delete-cities-" city-id-str)
           :btn-type :danger
           :shape "circle"
           :disabled delete-disabled?
           :on-click (fn [e]
                       (.stopPropagation e)
                       (when-not delete-disabled?
                         (confirm-dialog/show-confirm
                           {:title "Delete city"
                            :message "Do you want to delete this city?"
                            :on-confirm #(rf/dispatch [:user-expenses/delete-city city-id-str])
                            :on-cancel nil})))}
          ($ delete-icon))))))

(defui cities-page
  []
  (let [role (use-subscribe [:expenses/user-role])
        can-manage? (authz/can? role :expenses/cities.manage)
        entity-name :cities
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/refresh-cities-list]))
                       [])]
    (use-effect
      (fn []
        (rf/dispatch [::list-ui-state-events/set-pagination-mode entity-name :server])
        (rf/dispatch [::list-ui-state-events/set-refresh-event entity-name [:user-expenses/refresh-cities-list]])
        (refresh-list)
        js/undefined)
      [refresh-list])

    ($ expenses-page-guard
      {:capability :expenses/cities.manage
       :children
       ($ :div {:class "min-h-screen bg-base-100"}
         ($ :header {:class "bg-white border-b border-base-200"}
           ($ :div {:class "w-full px-4 py-4 sm:py-6"}
             ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
               ($ :div
                 ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "Cities")
                 ($ :p {:class "text-sm text-base-content/70"}
                   "Power-user city catalog (used for store normalization)."))
               ($ :div {:class "flex gap-2 flex-wrap"}
                 ($ button {:id "btn-back-expenses-dashboard-cities"
                            :btn-type :ghost
                            :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                   "Dashboard")))))

         ($ :main {:class "w-full px-4 py-6"}
           ($ list-view
             {:entity-name entity-name
              :entity-spec entity-spec
              :title "Cities"
              :form-display :modal
              :disallowed-action-mode :disable
              :allow-add? can-manage?
              :allow-edit? can-manage?
              :allow-delete? can-manage?
              :render-add-form render-add-form
              :render-edit-form render-edit-form
              :on-add-success refresh-list
              :on-edit-success refresh-list
              :render-actions render-actions})))})))