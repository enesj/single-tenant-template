 (ns app.domain.frontend.expenses.pages.user.payer-types
   "Admin/Owner-only Payer Types list."
   (:require
     [app.domain.frontend.expenses.authz :as authz]
     [app.domain.frontend.expenses.components.page-guard :refer [power-user-guard]]
     [app.domain.frontend.expenses.components.user-reference-forms :refer [user-payer-type-add-form-modal user-payer-type-edit-form-modal]]
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

(defui payer-types-page []
  (let [role (use-subscribe [:expenses/user-role])
        can-modify? (authz/power-user? role)
        entity-name :payer-types
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/refresh-payer-types-list]))
                       [])]

    (use-effect
      (fn []
        (rf/dispatch [::list-ui-state-events/set-pagination-mode entity-name :server])
        (rf/dispatch [::list-ui-state-events/set-refresh-event entity-name [:user-expenses/refresh-payer-types-list]])
        (refresh-list)
        js/undefined)
      [refresh-list])

    (let [render-edit-form
          (fn [item {:keys [on-success on-cancel]}]
            (let [payer-type-id (id-utils/extract-entity-id item)
                  initial-data (dissoc item :show-edit? :show-delete? :on-edit-click)]
              ($ user-payer-type-edit-form-modal
                {:payer-type-id payer-type-id
                 :initial-data initial-data
                 :on-success on-success
                 :on-cancel on-cancel})))

          render-add-form
          (fn [{:keys [on-success on-cancel]}]
            ($ user-payer-type-add-form-modal
              {:on-success on-success
               :on-cancel on-cancel}))

          render-actions
          (fn [item]
            (let [payer-type-id (id-utils/extract-entity-id item)
                  payer-type-id-str (some-> payer-type-id str)
                  on-edit-click (:on-edit-click item)
                  show-edit? (not (false? (:show-edit? item)))
                  show-delete? (not (false? (:show-delete? item)))
                  edit-disabled? (true? (:edit-disabled? item))
                  delete-disabled? (true? (:delete-disabled? item))
                  item-data (dissoc item :show-edit? :show-delete? :edit-disabled? :delete-disabled? :on-edit-click)]
              ($ :div {:class "flex items-center justify-center gap-2"}
                (when show-edit?
                  ($ button
                    {:id (str "btn-edit-payer-types-" payer-type-id-str)
                     :btn-type :primary
                     :shape "circle"
                     :disabled edit-disabled?
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (when (and (not edit-disabled?) on-edit-click)
                                   (on-edit-click item-data)))}
                    ($ edit-icon)))

                (when show-delete?
                  ($ button
                    {:id (str "btn-delete-payer-types-" payer-type-id-str)
                     :btn-type :danger
                     :shape "circle"
                     :disabled delete-disabled?
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (when-not delete-disabled?
                                   (confirm-dialog/show-confirm
                                     {:title "Delete payer type"
                                      :message "Do you want to delete this payer type?"
                                      :on-confirm #(rf/dispatch [:user-expenses/delete-payer-type payer-type-id-str])
                                      :on-cancel nil})))}
                    ($ delete-icon))))))]

      ($ power-user-guard
        {:children
         ($ :div {:class "min-h-screen bg-base-100"}
           ($ :header {:class "bg-white border-b border-base-200"}
             ($ :div {:class "w-full px-4 py-4 sm:py-6"}
               ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
                 ($ :div
                   ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "Payer Types")
                   ($ :p {:class "text-sm text-base-content/70"}
                     "Manage available payer types and default"))
                 ($ :div {:class "flex gap-2"}
                   ($ button {:id "btn-back-expenses-dashboard-payer-types"
                              :btn-type :ghost
                              :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                     "Dashboard")))))

           ($ :main {:class "w-full px-4 py-6"}
             ($ list-view
               {:entity-name entity-name
                :entity-spec entity-spec
                :title "Payer Types"
                :form-display :modal
                :disallowed-action-mode :disable
                :allow-add? can-modify?
                :allow-edit? can-modify?
                :allow-delete? can-modify?
                :render-add-form render-add-form
                :render-edit-form render-edit-form
                :render-actions render-actions})))}))))

