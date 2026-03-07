(ns app.domain.frontend.expenses.pages.user.expenses-list
  "User-facing expense list page.

   Uses template list-view UX but fetches data via user-scoped endpoints.

   IMPORTANT: This page is configured for :server pagination mode so sorting is
   always server-side across the full dataset (not just the currently loaded
   page)."
  (:require
    [app.domain.frontend.expenses.components.user-expense-form :refer [user-expense-add-form-modal
                                                                       user-expense-edit-form-modal]]
    [app.domain.frontend.expenses.pages.user.expense-detail :refer [expense-detail-page]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.dropdown.action :as dropdown]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon view-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.events.list.ui-state :as list-ui-state-events]
    [app.template.frontend.i18n :refer [use-t]]
    [app.template.frontend.utils.id :as id-utils]
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
  [t item {:keys [on-view]}]
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
                           {:title (t :expenses-list/delete-title)
                            :message (t :expenses-list/delete-msg)
                            :on-confirm #(rf/dispatch [:user-expenses/delete-expense expense-id])
                            :on-cancel nil})))
           :title (when delete-disabled? "Delete not available")}
          ($ delete-icon)))

      ($ dropdown/action-dropdown
        {:entity-id expense-id
         :trigger-label "⋯"
         :position :portal
         :actions
         [{:group-title (t :common/view)
           :items [{:id "view"
                    :icon ($ view-icon {:title (t :expenses-list/view-details)})
                    :label (t :expenses-list/view-details)
                    :on-click (fn [e]
                                (.stopPropagation e)
                                (if on-view
                                  (on-view item)
                                  (rf/dispatch [:navigate-to (str "/expenses/" expense-id)])))}]}]}))))

;; =============================================================================
;; Main Page
;; =============================================================================

(defui expenses-list-page
  []
  (let [t (use-t)
        entity-name :expenses
        [viewing-id set-viewing-id!] (use-state nil)
        ;; Use shared entity specs when available; fall back to nil which
        ;; list-view can still handle for basic rendering.
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        ;; Subscribe to current expense being viewed in modal
        current-expense (use-subscribe [:user-expenses/current-expense])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/refresh-expenses-list]))
                       [])]

    ;; Initial load + list-view wiring for server pagination/sorting
    (use-effect
      (fn []
        (rf/dispatch [::list-ui-state-events/set-pagination-mode :expenses :server])
        (rf/dispatch [::list-ui-state-events/set-refresh-event :expenses [:user-expenses/refresh-expenses-list]])
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
              ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} (t :expenses-list/title))
              ($ :p {:class "text-sm text-base-content/70"}
                (t :expenses-list/subtitle)))
            ($ :div {:class "flex gap-2"}
              ($ button {:btn-type :ghost
                         :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                (t :expenses-list/btn-dashboard))))))

      ;; Main content: list-view backed by shared entity store
      ($ :main {:class "w-full px-4 py-6"}
        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Expense"
           :render-add-form render-add-form
           :render-edit-form render-edit-form
           :on-add-success refresh-list
           :on-edit-success refresh-list
           :render-actions (fn [item]
                             (render-actions t item
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
