 (ns app.domain.frontend.expenses.pages.user.payer-types
   "Admin/Owner-only Payer Types list."
   (:require
     [app.domain.frontend.expenses.components.page-guard :refer [power-user-guard]]
     [app.domain.frontend.expenses.components.user-reference-forms :refer [user-payer-type-add-form-modal user-payer-type-edit-form-modal]]
     [app.template.frontend.components.button :refer [button]]
     [app.template.frontend.components.confirm-dialog :as confirm-dialog]
     [app.template.frontend.components.icons :refer [delete-icon edit-icon]]
     [app.template.frontend.components.list :refer [list-view]]
     [app.template.frontend.events.list.ui-state :as list-ui-state-events]
     [app.template.frontend.i18n :refer [use-t]]
     [app.template.frontend.utils.id :as id-utils]
     [re-frame.core :as rf]
     [uix.core :refer [$ defui use-callback use-effect]]
     [uix.re-frame :refer [use-subscribe]]
     app.domain.frontend.expenses.subs.user-expenses))

(defui payer-types-page []
  (let [t (use-t)
        entity-name :payer-types
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/refresh-payer-types-list]))
                       [])]

    (use-effect
      (fn []
        (rf/dispatch [::list-ui-state-events/set-pagination-mode entity-name :client])
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
                  is-system? (boolean (or (:is-system item) (:is_system item)))
                  show-edit? (and (not (false? (:show-edit? item))) (not is-system?))
                  show-delete? (and (not (false? (:show-delete? item))) (not is-system?))
                  edit-disabled? (or (true? (:edit-disabled? item)) is-system?)
                  delete-disabled? (or (true? (:delete-disabled? item)) is-system?)
                  item-data (dissoc item :show-edit? :show-delete? :edit-disabled? :delete-disabled? :on-edit-click)]
              ($ :div {:class "flex items-center justify-center gap-2"}
                (when is-system?
                  ($ :span {:class "ds-badge ds-badge-sm ds-badge-neutral"} "System"))
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
                                     {:title (t :payer-types/delete-title)
                                      :message (t :payer-types/delete-msg)
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
                   ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} (t :payer-types/title))
                   ($ :p {:class "text-sm text-base-content/70"}
                     (t :payer-types/subtitle)))
                 ($ :div {:class "flex gap-2"}
                   ($ button {:id "btn-back-expenses-dashboard-payer-types"
                              :btn-type :ghost
                              :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                     (t :payer-types/btn-dashboard))))))

           ($ :main {:class "w-full px-4 py-6"}
             ($ list-view
               {:entity-name entity-name
                :entity-spec entity-spec
                :render-add-form render-add-form
                :render-edit-form render-edit-form
                :render-actions render-actions})))}))))

