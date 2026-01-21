(ns app.domain.frontend.expenses.pages.user.expenses-list
  "User-facing expense list page with filtering and pagination.

   Implements admin-like UX (modal add/edit) while using user-scoped endpoints."
  (:require
    [app.domain.frontend.expenses.components.user-expense-form :refer [user-expense-add-form-modal
                                                                       user-expense-edit-form-modal]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.dropdown.action :as dropdown]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon view-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]
    ;; Ensure subs are registered
    app.domain.frontend.expenses.subs.user-expenses))

;; =============================================================================
;; Modal form renderers
;; =============================================================================

(defn- render-add-form
  [{:keys [on-success on-cancel]}]
  ($ user-expense-add-form-modal
    {:on-success on-success
     :on-cancel on-cancel}))

(defn- render-edit-form
  [item {:keys [on-success on-cancel]}]
  (let [expense-id (id-utils/extract-entity-id item)]
    ;; IMPORTANT: do NOT pass list-row data as :initial-data.
    ;;
    ;; The list row is intentionally "summary" data and often does not include
    ;; nested detail like :items. Fork forms do not re-initialize when initial
    ;; values change, so the first open could render without line items.
    ;;
    ;; We let the modal fetch the full detail and mount the form once it is loaded.
    ($ user-expense-edit-form-modal
      {:expense-id expense-id
       :initial-data nil
       :on-success on-success
       :on-cancel on-cancel})))

(defn- render-actions
  "Row action dropdown (admin-style) for user expenses.

   Uses list-view's modal edit handler when available, and user-scoped delete.
   Edit/delete buttons remain visible but disabled when disallowed (edit-disabled?/delete-disabled?)."
  [item]
  (let [expense-id (id-utils/extract-entity-id item)
        on-edit-click (:on-edit-click item)
        show-edit? (not (false? (:show-edit? item)))
        show-delete? (not (false? (:show-delete? item)))
        item-data (dissoc item :show-edit? :show-delete? :edit-disabled? :delete-disabled? :on-edit-click)
        edit-disabled? (true? (:edit-disabled? item))
        delete-disabled? (true? (:delete-disabled? item))]
    ($ :div {:class "flex items-center justify-center gap-2"}
      (when show-edit?
        ($ button
          {:id (str "btn-edit-expenses-" expense-id)
           :btn-type :primary
           :shape "circle"
           :disabled edit-disabled?
           :on-click (fn [e]
                       (.stopPropagation e)
                       (when-not edit-disabled?
                         (if on-edit-click
                           (on-edit-click item-data)
                           (rf/dispatch [:navigate-to (str "/expenses/" expense-id "?edit=true")]))))}
          ($ edit-icon)))

      (when show-delete?
        ($ button
          {:id (str "btn-delete-expenses-" expense-id)
           :btn-type :danger
           :shape "circle"
           :disabled delete-disabled?
           :on-click (fn [e]
                       (.stopPropagation e)
                       (when-not delete-disabled?
                         (confirm-dialog/show-confirm
                           {:title "Delete expense"
                            :message "Do you want to delete this expense?"
                            :on-confirm #(rf/dispatch [:user-expenses/delete-expense expense-id])
                            :on-cancel nil})))}
          ($ delete-icon)))

      ($ dropdown/action-dropdown
        {:entity-id expense-id
         :trigger-label "⋯"
         :position :portal
         :actions
         [{:group-title "View"
           :items [{:id "view"
                    :icon ($ view-icon {:title "View"})
                    :label "View Details"
                    :on-click (fn [_e]
                                (rf/dispatch [:navigate-to (str "/expenses/" expense-id)]))}]}]}))))

;; =============================================================================
;; Main Page
;; =============================================================================

(defui expenses-list-page []
  (let [entity-name :expenses
        ;; Use shared entity specs when available; fall back to nil which
        ;; list-view can still handle for basic rendering.
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        error (use-subscribe [:user-expenses/recent-error])
        can-write? (use-subscribe [:expenses/can-write?])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/fetch-recent {:limit 25 :offset 0}]))
                       [])]

    ;; Ensure we kick off a user-scoped fetch so that the shared
    ;; template entity store for :expenses and its FK references is
    ;; populated via the user-expenses pipeline and the expenses
    ;; adapter sync events.
    (use-effect
      (fn []
        (refresh-list)
        ;; Reference data for FK columns in the list-view (supplier & payer)
        (rf/dispatch [:user-expenses/fetch-suppliers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        js/undefined)
      [refresh-list])

    ($ :div {:class "min-h-screen bg-base-100"}
      ;; Header
      ($ :header {:class "bg-white border-b border-base-200"}
        ($ :div {:class "w-full px-4 py-4 sm:py-6"}
          ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
            ($ :div
              ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "My Expenses")
              ($ :p {:class "text-sm text-base-content/70"}
                "View and manage your expense history"))
            ($ :div {:class "flex gap-2"}
              ($ button {:btn-type :ghost
                         :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                "Dashboard")))))

      ;; Error banner (from user-expenses pipeline)
      (when error
        ($ :div {:class "w-full px-4 mt-4"}
          ($ :div {:class "ds-alert ds-alert-error"}
            ($ :span error))))

      ;; Main content: list-view backed by shared entity store
      ($ :main {:class "w-full px-4 py-6"}
        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Expense"
           :form-display :modal
           :disallowed-action-mode :disable
           :allow-add? can-write?
           :allow-edit? can-write?
           :allow-delete? can-write?
           :render-add-form render-add-form
           :render-edit-form render-edit-form
           :on-add-success refresh-list
           :on-edit-success refresh-list
           :render-actions render-actions})))))
