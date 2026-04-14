(ns app.admin.frontend.pages.domain.expenses.expenses
  "Admin Expenses page.

  Renders an admin-native list backed by the expenses admin API."
  (:require
    [app.admin.frontend.components.layout :as layout]
    app.admin.frontend.events.receipts-approval
    app.admin.frontend.events.receipts-detail
    app.admin.frontend.subs.receipts-detail
    [app.admin.frontend.pages.domain.expenses.receipts :as admin-receipts-page]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.icons :refer [edit-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.list.cells :as list-cells]
    [app.template.frontend.events.list.ui-state :as ui-state]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]

    ;; Side-effect requires: register sync handlers, entity spec fallbacks, CRUD bridges,
    ;; and list-loading events.
    app.domain.frontend.expenses.admin.adapters.admin-crud
    app.domain.frontend.expenses.admin.adapters.specs
    app.domain.frontend.expenses.admin.adapters.sync
    [app.domain.frontend.expenses.events.expenses :as expenses-events]))

(defn- expense-receipt-id
  [expense]
  (or (:receipt-id expense)
    (:receipt_id expense)))

(defn- manual-expense?
  [expense]
  (nil? (expense-receipt-id expense)))

(defn- render-expense-actions
  [open-receipt-detail! expense]
  (let [expense-id (id-utils/extract-entity-id expense)
        receipt-id (some-> (expense-receipt-id expense) str)
        manual? (manual-expense? expense)
        show-edit? (not (false? (:show-edit? expense)))
        show-delete? (not (false? (:show-delete? expense)))
        edit-disabled? (boolean (:edit-disabled? expense))
        delete-disabled? (boolean (:delete-disabled? expense))]
    ($ :div {:class "flex items-center gap-2"}
      (when show-edit?
        (if manual?
          ($ list-cells/edit-button
            {:entity-name :expenses
             :item-id expense-id
             :item expense
             :disabled? edit-disabled?
             :on-edit-click (:on-edit-click expense)})
          ($ button
            {:id (str "btn-edit-receipt-for-expense-" expense-id)
             :btn-type :primary
             :shape "circle"
             :title "Edit linked receipt"
             :disabled edit-disabled?
             :on-click (fn [e]
                         (.stopPropagation e)
                         (when-not edit-disabled?
                           (open-receipt-detail! receipt-id)))}
            ($ edit-icon))))
      (when show-delete?
        ($ list-cells/delete-button
          {:entity-name :expenses
           :item-id expense-id
           :disabled? delete-disabled?})))))

(defui admin-expenses-page
  "Admin route: /admin/expenses"
  []
  (let [entity-name :expenses
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        [detail-open? set-detail-open!] (use-state false)
        [detail-receipt-id set-detail-receipt-id!] (use-state nil)
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch-sync [::ui-state/set-pagination-mode entity-name :server])
                         (rf/dispatch-sync [::ui-state/set-refresh-event entity-name
                                            [::expenses-events/load-list]])
                         (rf/dispatch [::expenses-events/load-list {:page 1}]))
                       [])
        open-receipt-detail! (use-callback
                               (fn [receipt-id]
                                 (when-let [receipt-id* (some-> receipt-id str)]
                                   (set-detail-receipt-id! receipt-id*)
                                   (set-detail-open! true)
                                   (rf/dispatch [:admin/fetch-receipt-detail receipt-id*])))
                               [])
        close-receipt-detail! (use-callback
                                (fn []
                                  (set-detail-open! false)
                                  (set-detail-receipt-id! nil))
                                [])]
    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    ($ layout/admin-layout
      ($ :div {:class "p-6 min-h-screen"}
        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Expenses"
           :render-actions #(render-expense-actions open-receipt-detail! %)})
        ($ admin-receipts-page/admin-receipt-detail-modal
          {:open? detail-open?
           :receipt-id detail-receipt-id
           :on-close close-receipt-detail!})))))
