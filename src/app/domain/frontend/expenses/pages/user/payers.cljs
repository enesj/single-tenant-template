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
  [item {:keys [on-success on-cancel]}]
  (let [payer-id (id-utils/extract-entity-id item)
        initial-data (dissoc item :show-edit? :show-delete? :on-edit-click)]
    ($ user-payer-edit-form-modal
      {:payer-id payer-id
       :initial-data initial-data
       :on-success on-success
       :on-cancel on-cancel})))

(defn- render-add-form
  [{:keys [on-success on-cancel]}]
  ($ user-payer-add-form-modal
    {:on-success on-success
     :on-cancel on-cancel}))

(defui payers-page []
  (let [t (use-t)
        role (use-subscribe [:expenses/user-role])
        can-modify? (authz/can? role :expenses/reference.write)
        entity-name :payers
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        payers-entity-spec (mapv (fn [field]
                                   (let [fid (:id field)
                                         fid-name (cond
                                                    (keyword? fid) (name fid)
                                                    (string? fid) fid
                                                    :else (str fid))]
                                     (if (#{"payer_type_id" "payer-type-id"}
                                          fid-name)
                                       (assoc field :type "select" :options [:payer-types :label])
                                       field)))
                             (or entity-spec []))
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
                  is-system-payer? (boolean (or (:payer-type-is-system item) (:payer_type_is_system item)))
                  is-active? (let [v (if (contains? item :is-active) (:is-active item) (get item :is_active true))]
                               (not (false? v)))
                  show-edit? (not (false? (:show-edit? item)))
                  show-delete? (and (not (false? (:show-delete? item))) (not is-system-payer?))
                  edit-disabled? (true? (:edit-disabled? item))
                  delete-disabled? (or (true? (:delete-disabled? item)) is-system-payer?)
                  item-data (dissoc item :show-edit? :show-delete? :edit-disabled? :delete-disabled? :on-edit-click)]
              ($ :div {:class "flex items-center justify-center gap-2"}
                (when is-system-payer?
                  ($ :span {:class "ds-badge ds-badge-sm ds-badge-neutral"} "System"))
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
             :entity-spec payers-entity-spec
             :title "Payers"
             :render-add-form render-add-form
             :render-edit-form render-edit-form
             :render-actions render-actions}))))))
