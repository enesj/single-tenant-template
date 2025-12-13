(ns app.domain.frontend.expenses.pages.user.expenses-list
  "User-facing expense list page with filtering and pagination.

  Implements admin-like UX (modal add/edit) while using user-scoped endpoints."
  (:require
    [app.domain.frontend.expenses.components.user-expense-form :refer [user-expense-add-form-modal
                                                                       user-expense-edit-form-modal]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [delete-icon view-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]
    ;; Ensure subs are registered
    [app.template.frontend.subs.user-expenses]))

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
    ($ user-expense-edit-form-modal
      {:expense-id expense-id
       :initial-data item
       :on-success on-success
       :on-cancel on-cancel})))

(defn- custom-actions
  "Extra row actions rendered alongside the standard edit button.

  NOTE: We do not use the template delete button here because it is wired to
  template CRUD events; user delete uses user-scoped events."
  [item]
  (let [expense-id (id-utils/extract-entity-id item)]
    ($ :<>
      ;; View details
      ($ button
        {:btn-type :ghost
         :shape "circle"
         :on-click (fn [e]
                     (.stopPropagation e)
                     (rf/dispatch [:navigate-to (str "/expenses/" expense-id)]))}
        ($ view-icon {:title "View"}))

      ;; Delete
      ($ button
        {:btn-type :danger
         :shape "circle"
         :on-click (fn [e]
                     (.stopPropagation e)
                     (confirm-dialog/show-confirm
                       {:title "Delete expense"
                        :message "Do you want to delete this expense?"
                        :on-confirm #(rf/dispatch [:user-expenses/delete-expense expense-id])
                        :on-cancel nil}))}
        ($ delete-icon)))))

;; =============================================================================
;; Main Page
;; =============================================================================

(defui expenses-list-page []
  (let [entity-name :expenses
        ;; Use shared entity specs when available; fall back to nil which
        ;; list-view can still handle for basic rendering.
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        error (use-subscribe [:user-expenses/recent-error])
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
              ;; Keep the dedicated page for now; modal add is available in the table.
              ($ button {:btn-type :primary
                         :on-click #(rf/dispatch [:navigate-to "/expenses/new"])}
                "+ New Expense")
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
           :render-add-form render-add-form
           :render-edit-form render-edit-form
           :on-add-success refresh-list
           :on-edit-success refresh-list
           :custom-actions custom-actions
           ;; User-facing defaults
           :display-settings {:show-select? false
                              :show-edit? true
                              :show-delete? false
                              :show-filtering? true
                              :show-pagination? true
                              :show-add-button? true
                              :show-batch-edit? false
                              :show-batch-delete? false
                              :show-timestamps? true
                              :show-highlights? true
                              :per-page 25}})))))
