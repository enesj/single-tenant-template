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
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.events.list.ui-state :as ui-events]
    [app.template.frontend.utils.id :as id-utils]
    [app.domain.frontend.expenses.pages.user.expense-detail :refer [expense-detail-page]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect use-state]]
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

   Supports an optional :on-view callback for opening a custom modal."
  [item {:keys [on-view]}]
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
                                (if on-view
                                  (on-view item)
                                  (rf/dispatch [:navigate-to (str "/expenses/" expense-id)])))}]}]}))))

;; =============================================================================
;; Main Page
;; =============================================================================

(defui expenses-list-page []
  (let [entity-name :expenses
        [viewing-id set-viewing-id!] (use-state nil)
        ;; Use shared entity specs when available; fall back to nil which
        ;; list-view can still handle for basic rendering.
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        recent-items (or (use-subscribe [:user-expenses/recent]) [])
        recent-page (or (use-subscribe [:user-expenses/recent-page]) 1)
        recent-total-pages (or (use-subscribe [:user-expenses/recent-total-pages]) 1)
        recent-limit (or (use-subscribe [:user-expenses/recent-limit]) 25)
        error (use-subscribe [:user-expenses/recent-error])
        can-write? (use-subscribe [:expenses/can-write?])
        ;; Subscribe to current expense being viewed in modal
        current-expense (use-subscribe [:user-expenses/current-expense])
        go-to-page (use-callback
                     (fn [page]
                       (rf/dispatch [:user-expenses/recent-go-to-page
                                     {:page page}]))
                     [])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/recent-go-to-page
                                       {:page 1}]))
                       [])
        handle-per-page-change (use-callback
                                 (fn [new-limit]
                                   (rf/dispatch [::ui-events/set-per-page :expenses new-limit])
                                   (rf/dispatch [:user-expenses/recent-go-to-page
                                                 {:page 1
                                                  :limit new-limit}]))
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
        (rf/dispatch [:user-expenses/fetch-expense-categories {:limit 500 :offset 0}])
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
           :per-page recent-limit
           :rows-override recent-items
           :pagination-override {:current-page recent-page
                                 :total-pages recent-total-pages
                                 :on-page-change go-to-page
                                 :on-per-page-change handle-per-page-change}
           :form-display :modal
           :disallowed-action-mode :disable
           :allow-add? can-write?
           :allow-edit? can-write?
           :allow-delete? can-write?
           :render-add-form render-add-form
           :render-edit-form render-edit-form
           :on-add-success refresh-list
           :on-edit-success refresh-list
           :render-actions (fn [item]
                             (render-actions item
                               {:on-view #(set-viewing-id! (id-utils/extract-entity-id %))}))}))

      (when viewing-id
        (let [supplier-name (or (:supplier_display_name current-expense) "Expense Details")]
          ($ modal-wrapper
            {:id (str "modal-view-" viewing-id)
             :visible? true
             :title supplier-name
             :breadcrumbs [{:label "Expenses" :href "/expenses"}
                           {:label "All Expenses" :href "/expenses/list"}
                           {:label supplier-name}]
             :size :large
             :draggable? true
             :resizable? true
             :close-button-id (str "btn-close-expense-details-" viewing-id)
             :on-close #(set-viewing-id! nil)}
            ($ expense-detail-page {:expense-id viewing-id
                                    :in-modal? true})))))))