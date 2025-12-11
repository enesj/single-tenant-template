(ns app.template.frontend.pages.expenses-list
  "User-facing expense list page with filtering and pagination."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [edit-icon delete-icon]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state use-effect]]
    [uix.re-frame :refer [use-subscribe]]
    [app.template.frontend.subs.user-expenses]))

;; ========================================================================
;; Formatting helpers
;; ========================================================================

;; ========================================================================
;; Table Components
;; ========================================================================

;; ========================================================================
;; Pagination Component
;; ========================================================================

;; ========================================================================
;; Filter Component
;; ========================================================================

;; ========================================================================
;; Main Page
;; ========================================================================

(defui my-expense-actions
  [{:keys [id show-edit? show-delete?] :as item}]
  (let [expense-id (or id (:id item))]
    (when expense-id
      ($ :div {:class "flex items-center justify-end gap-2"}
        (when show-edit?
          ($ button
            {:btn-type :primary
             :shape "circle"
             :on-click (fn [e]
                         (.stopPropagation e)
                         (rf/dispatch [:navigate-to (str "/expenses/" expense-id)]))}
            ($ edit-icon)))
        (when show-delete?
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
            ($ delete-icon)))))))

(defui expenses-list-page []
  (let [entity-name :expenses
        ;; Use shared entity specs when available; fall back to nil which
        ;; list-view can still handle for basic rendering.
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        error (use-subscribe [:user-expenses/recent-error])]

    ;; Ensure we kick off a user-scoped fetch so that the shared
    ;; template entity store for :expenses and its FK references is
    ;; populated via the user-expenses pipeline and the expenses
    ;; admin adapter sync events.
    (use-effect
      (fn []
        ;; Primary list data – user-scoped expenses
        (rf/dispatch [:user-expenses/fetch-recent {:limit 25 :offset 0}])
        ;; Reference data for FK columns in the list-view (supplier & payer)
        (rf/dispatch [:user-expenses/fetch-suppliers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        js/undefined)
      [])

    ($ :div {:class "min-h-screen bg-base-100"}
      ;; Header
      ($ :header {:class "bg-white border-b border-base-200"}
        ($ :div {:class "max-w-6xl mx-auto px-4 py-4 sm:py-6"}
          ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
            ($ :div
              ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "My Expenses")
              ($ :p {:class "text-sm text-base-content/70"}
                "View and manage your expense history"))
            ($ :div {:class "flex gap-2"}
              ($ button {:btn-type :primary
                         :on-click #(rf/dispatch [:navigate-to "/expenses/new"])}
                "+ New Expense")
              ($ button {:btn-type :ghost
                         :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                "Dashboard")))))

      ;; Error banner (from user-expenses pipeline)
      (when error
        ($ :div {:class "max-w-6xl mx-auto px-4 mt-4"}
          ($ :div {:class "ds-alert ds-alert-error"}
            ($ :span error))))

      ;; Main content: generic list-view backed by shared entity store
      ($ :main {:class "max-w-6xl mx-auto px-4 py-6"}
        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "My Expenses"
           :render-actions my-expense-actions
           ;; User-facing defaults – no bulk selection/edit/delete, but keep
           ;; filtering + pagination.
           :display-settings {:show-select? false
                              :show-edit? false
                              :show-delete? false
                              :show-filtering? true
                              :show-pagination? true
                              :show-add-button? false
                              :per-page 25}})))))
