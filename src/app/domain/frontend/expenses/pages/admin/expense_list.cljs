(ns app.domain.frontend.expenses.pages.admin.expense-list
  "Admin expense list page with custom modal forms for add/edit."
  (:require
    [app.domain.frontend.expenses.components.expense-form :refer [expense-add-form-modal
                                                                  expense-edit-form-modal]]
    [app.domain.frontend.expenses.events.expenses :as expenses-events]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

;; =============================================================================
;; Entity Spec for Expenses List Display
;; =============================================================================

(def expenses-entity-spec
  "Column configuration for expenses list view.
   This defines what columns are shown in the table.
   Keys must match the kebab-case keys in the entity store."
  [{:id :id
    :label "ID"
    :type :uuid
    :admin {:display-order 1}}
   {:id :supplier-display-name
    :label "Supplier"
    :type :text
    :admin {:display-order 2}}
   {:id :payer-label
    :label "Payer"
    :type :text
    :admin {:display-order 3}}
   {:id :total-amount
    :label "Total"
    :type :decimal
    :admin {:display-order 4}}
   {:id :currency
    :label "Currency"
    :type :text
    :admin {:display-order 5}}
   {:id :purchased-at
    :label "Purchased At"
    :type :datetime
    :admin {:display-order 6}}])

;; =============================================================================
;; Display Settings
;; =============================================================================

(def expenses-display-settings
  {:show-select? true
   :show-edit? true
   :show-delete? false
   :show-filtering? true
   :show-pagination? true
   :show-add-button? true
   :show-batch-edit? false
   :show-batch-delete? false
   :show-timestamps? true
   :show-highlights? true
   :per-page 25})

;; =============================================================================
;; Custom Form Renderers for Modal
;; =============================================================================

(defn render-add-form
  "Renders the expense add form for modal display."
  [{:keys [on-success on-cancel]}]
  ($ expense-add-form-modal
    {:on-success on-success
     :on-cancel on-cancel}))

(defn render-edit-form
  "Renders the expense edit form for modal display.
   Item contains the row data from the list."
  [item {:keys [on-success on-cancel]}]
  (let [expense-id (id-utils/extract-entity-id item)]
    ($ expense-edit-form-modal
      {:expense-id expense-id
       :initial-data item
       :on-success on-success
       :on-cancel on-cancel})))

;; =============================================================================
;; Main Page Component
;; =============================================================================

(defui admin-expense-list-page []
  (let [;; Refresh list callback
        refresh-list (fn []
                       (rf/dispatch [::expenses-events/load-list {}]))]

    ;; Load expenses on mount
    (use-effect
      (fn []
        (rf/dispatch [::expenses-events/load-list {}])
        js/undefined)
      [])

    ($ :div {:class "ds-card ds-bg-base-100 ds-shadow-xl"}
      ($ :div {:class "ds-card-body p-0"}
        ($ :div {:class "w-full pb-0 [&>div>table]:w-full"}
          ($ list-view
            {:entity-name :expenses
             :entity-spec expenses-entity-spec
             :title "Expenses"
             :display-settings expenses-display-settings
             ;; Enable modal forms
             :form-display :modal
             :render-add-form render-add-form
             :render-edit-form render-edit-form
             ;; Success callbacks refresh the list
             :on-add-success refresh-list
             :on-edit-success refresh-list}))))))
