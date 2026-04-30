(ns app.admin.frontend.pages.domain.expenses.expenses
  "Admin Expenses page.

  Renders an admin-native list backed by the expenses admin API."
  (:require
    [app.admin.frontend.components.layout :as layout]
    app.admin.frontend.events.receipts-approval
    app.admin.frontend.events.receipts-detail
    app.admin.frontend.subs.receipts-detail
    [app.admin.frontend.pages.domain.expenses.receipts :as admin-receipts-page]
    [app.domain.frontend.expenses.components.user-expense-form :as user-expense-form]
    [app.domain.frontend.expenses.events.expense-items :as expense-items-events]
    [app.domain.frontend.expenses.subs.expense-items :as expense-items-subs]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.icons :refer [chevron-right-icon edit-icon]]
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
    ;; Registers :admin-expenses/refresh-list (depends on events.expenses for ::load-list)
    app.domain.frontend.expenses.events.source-filter))

(defn- expense-receipt-id
  [expense]
  (or (:receipt-id expense)
    (:receipt_id expense)))

(defn- manual-expense?
  [expense]
  (nil? (expense-receipt-id expense)))

(defn- expense-item-count
  [expense]
  (or (:item-count expense)
    (:item_count expense)
    0))

(defn- render-expense-actions
  [open-receipt-detail! toggle-expand! expanded-ids expense]
  (let [expense-id (id-utils/extract-entity-id expense)
        receipt-id (some-> (expense-receipt-id expense) str)
        manual? (manual-expense? expense)
        item-count (expense-item-count expense)
        expanded? (contains? expanded-ids expense-id)
        show-expand? (pos? item-count)
        show-edit? (not (false? (:show-edit? expense)))
        show-delete? (not (false? (:show-delete? expense)))
        edit-disabled? (boolean (:edit-disabled? expense))
        delete-disabled? (boolean (:delete-disabled? expense))]
    ($ :div {:class "flex items-center gap-2"}
      (when show-expand?
        ($ button
          {:id (str "btn-expand-expenses-" expense-id)
           :btn-type :ghost
           :shape "circle"
           :title (if expanded? "Collapse items" "Expand items")
           :on-click (fn [e]
                       (.stopPropagation e)
                       (toggle-expand! expense-id))}
          ($ :span {:class (str "inline-flex transition-transform duration-150"
                             (when expanded? " rotate-90"))}
            ($ chevron-right-icon))))
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

(defui expense-items-expand-row
  "Renders line items for a single admin expense in readonly mode."
  [{:keys [expense-id]}]
  (let [items (use-subscribe [::expense-items-subs/items-for-expense expense-id])
        loading? (use-subscribe [::expense-items-subs/loading-for-expense? expense-id])]
    ($ :div {:class "px-6 py-3 bg-base-50 border-b border-base-200"}
      (if loading?
        ($ :div {:class "space-y-2 py-1"}
          (for [i (range 3)]
            ($ :div {:key i :class "h-4 bg-base-200 rounded animate-pulse"})))
        (if (empty? items)
          ($ :div {:class "text-sm text-base-content/50 py-1"} "No items")
          ($ :table {:class "w-full text-sm"}
            ($ :thead
              ($ :tr {:class "text-left text-xs text-base-content/50 uppercase border-b border-base-200"}
                ($ :th {:class "py-1.5 pr-4 font-medium"} "Article")
                ($ :th {:class "py-1.5 pr-4 font-medium text-right"} "Qty")
                ($ :th {:class "py-1.5 pr-4 font-medium text-right"} "Unit Price")
                ($ :th {:class "py-1.5 font-medium text-right"} "Total")))
            ($ :tbody
              (for [item items]
                ($ :tr {:key (or (:id item) (str item))
                        :class "border-b border-base-100 last:border-0"}
                  ($ :td {:class "py-1.5 pr-4 text-base-content"}
                    (or (:article-canonical-name item)
                      (:article_canonical_name item)
                      ($ :span {:class "text-base-content/30"} "—")))
                  ($ :td {:class "py-1.5 pr-4 text-right tabular-nums text-base-content/70"}
                    (or (:qty item) "—"))
                  ($ :td {:class "py-1.5 pr-4 text-right tabular-nums text-base-content/70"}
                    (if-let [up (or (:unit-price item) (:unit_price item))]
                      (str up)
                      "—"))
                  ($ :td {:class "py-1.5 text-right tabular-nums font-medium"}
                    (or (:line-total item) (:line_total item) "—")))))))))))

(defui admin-expenses-page
  "Admin route: /admin/expenses"
  []
  (let [entity-name :expenses
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        [detail-open? set-detail-open!] (use-state false)
        [detail-receipt-id set-detail-receipt-id!] (use-state nil)
        [expanded-ids set-expanded-ids!] (use-state #{})
        {:keys [show-manual? show-receipts?]} (use-subscribe [:expenses/source-filter])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch-sync [::ui-state/set-pagination-mode entity-name :server])
                         (rf/dispatch-sync [::ui-state/set-refresh-event entity-name
                                            [:admin-expenses/refresh-list]])
                         (rf/dispatch [:admin-expenses/refresh-list {:page 1}]))
                       [])
        open-receipt-detail! (use-callback
                               (fn [receipt-id]
                                 (when-let [receipt-id* (some-> receipt-id str)]
                                   (set-detail-receipt-id! receipt-id*)
                                   (set-detail-open! true)
                                   (rf/dispatch [:admin/fetch-receipt-detail receipt-id*])))
                               [])
        toggle-expand! (use-callback
                         (fn [expense-id]
                           (set-expanded-ids!
                             (fn [ids]
                               (if (contains? ids expense-id)
                                 (disj ids expense-id)
                                 (conj ids expense-id))))
                           (rf/dispatch [::expense-items-events/fetch-admin-items-for-expense expense-id]))
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
           :form-display :modal
           :row-class-fn (fn [item]
                           (when (manual-expense? item) "bg-slate-200/70"))
           :extra-settings-toggle-groups
           [{:id "toggle-group-expense-source"
             :toggles [{:id "toggle-show-manual-expenses"
                        :label "Manual"
                        :active? show-manual?
                        :on-click #(rf/dispatch [:expenses/toggle-source-filter
                                                 :manual
                                                 [:admin-expenses/refresh-list]])}
                       {:id "toggle-show-receipt-expenses"
                        :label "From receipts"
                        :active? show-receipts?
                        :on-click #(rf/dispatch [:expenses/toggle-source-filter
                                                 :receipts
                                                 [:admin-expenses/refresh-list]])}]}]
           :render-actions #(render-expense-actions open-receipt-detail! toggle-expand! expanded-ids %)
           :render-row-expansion (fn [item]
                                   (let [expense-id (id-utils/extract-entity-id item)]
                                     (when (and (pos? (expense-item-count item))
                                             (contains? expanded-ids expense-id))
                                       ($ expense-items-expand-row {:expense-id expense-id}))))
           :render-edit-form
           (fn [item {:keys [on-success on-cancel]}]
             ($ user-expense-form/user-expense-edit-form-modal
               {:expense-id (id-utils/extract-entity-id item)
                :initial-data item
                :on-success (fn []
                              (when on-success (on-success))
                              (refresh-list))
                :on-cancel on-cancel}))})
        ($ admin-receipts-page/admin-receipt-detail-modal
          {:open? detail-open?
           :receipt-id detail-receipt-id
           :on-close close-receipt-detail!})))))
