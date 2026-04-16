(ns app.domain.frontend.expenses.pages.user.payers
  "User-facing payers list (shared catalog)."
  (:require
    [app.domain.frontend.expenses.authz :as authz]
    [app.domain.frontend.expenses.components.user-reference-forms :refer [user-payer-add-form-modal user-payer-edit-form-modal]]
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

(defn- render-edit-form
  [label-only? item {:keys [on-success on-cancel]}]
  (let [payer-id (id-utils/extract-entity-id item)
        initial-data (dissoc item :show-edit? :show-delete? :on-edit-click)]
    ($ user-payer-edit-form-modal
      {:payer-id payer-id
       :initial-data initial-data
       :label-only? label-only?
       :on-success on-success
       :on-cancel on-cancel})))

(defn- render-add-form
  [{:keys [on-success on-cancel]}]
  ($ user-payer-add-form-modal
    {:on-success on-success
     :on-cancel on-cancel}))

(def ^:private system-payer-row-border-class
  "[&>td]:border-y [&>td]:border-y-primary/20 [&>td:first-child]:border-l-4 [&>td:first-child]:border-l-primary [&>td:last-child]:border-r [&>td:last-child]:border-r-primary/20")

(defn- system-payer-row?
  [item]
  (boolean (or (:payer-type-is-system item)
             (:payer_type_is_system item))))

(defn- payer-row-class
  [item]
  (when (system-payer-row? item)
    system-payer-row-border-class))

(defui payers-page []
  (let [t (use-t)
        role (use-subscribe [:expenses/user-role])
        can-modify? (authz/can? role :expenses/reference.write)
        can-manage? (authz/power-user? role)
        user-payer-id (use-subscribe [:user-expenses/user-payer-id])
        entity-name :payers
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/refresh-payers-list])
                         (rf/dispatch [:user-expenses/fetch-payer-types]))
                       [])]

    (use-effect
      (fn []
        (rf/dispatch [::list-ui-state-events/set-pagination-mode entity-name :client])
        (refresh-list)
        js/undefined)
      [refresh-list])

    (let [render-actions
          (fn [item]
            (let [payer-id (id-utils/extract-entity-id item)
                  payer-id-str (some-> payer-id str)
                  on-edit-click (:on-edit-click item)
                  is-system-payer? (system-payer-row? item)
                  is-active? (let [v (if (contains? item :is-active) (:is-active item) (get item :is_active true))]
                               (not (false? v)))
                  is-own-payer? (and (some? user-payer-id) (= payer-id-str user-payer-id))
                  ;; admin/owner: edit any non-system payer; member: edit only own payer
                  show-edit? (if can-manage?
                               (not is-system-payer?)
                               is-own-payer?)
                  ;; delete: admin/owner only, non-system payers only
                  show-delete? (and can-manage? (not is-system-payer?))
                  edit-disabled? (true? (:edit-disabled? item))
                  delete-disabled? (true? (:delete-disabled? item))
                  item-data (dissoc item :show-edit? :show-delete? :edit-disabled? :delete-disabled? :on-edit-click)]
              ($ :div {:class "flex items-center justify-center gap-2"}
                (when-not is-active?
                  ($ :span {:class "ds-badge ds-badge-sm ds-badge-warning"} "Inactive"))
                (when show-edit?
                  ($ button
                    {:id (str "btn-edit-payers-" payer-id-str)
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
                    {:id (str "btn-delete-payers-" payer-id-str)
                     :btn-type :danger
                     :shape "circle"
                     :disabled delete-disabled?
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (when-not delete-disabled?
                                   (confirm-dialog/show-confirm
                                     {:title (t :payers/delete-title)
                                      :message (t :payers/delete-msg)
                                      :on-confirm #(rf/dispatch [:user-expenses/delete-payer payer-id-str])
                                      :on-cancel nil})))}
                    ($ delete-icon))))))]

      ($ :div {:class "min-h-screen bg-base-100"}
        ($ :header {:class "bg-white border-b border-base-200"}
          ($ :div {:class "w-full px-4 py-4 sm:py-6"}
            ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
              ($ :div
                ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} (t :payers/title))
                ($ :p {:class "text-sm text-base-content/70"}
                  (t :payers/subtitle)))
              ($ :div {:class "flex gap-2"}
                ($ button {:id "btn-back-expenses-dashboard-payers"
                           :btn-type :ghost
                           :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                  (t :payers/btn-dashboard))))))

        (when (not can-modify?)
          ($ :div {:class "w-full px-4 mt-4"}
            ($ :div {:class "ds-alert"}
              ($ :span (t :payers/read-only-notice)))))

        ($ :main {:class "w-full px-4 py-6"}
          ($ list-view
            {:entity-name entity-name
             :entity-spec entity-spec
             ;; only admin/owner can add new payers
             :render-add-form (when can-manage? render-add-form)
             ;; members get label-only edit form; admin/owner get full form
             :render-edit-form (partial render-edit-form (not can-manage?))
             :render-actions render-actions
             :row-class-fn payer-row-class}))))))
