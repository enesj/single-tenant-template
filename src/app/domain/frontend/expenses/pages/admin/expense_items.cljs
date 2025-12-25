(ns app.domain.frontend.expenses.pages.admin.expense-items
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [app.domain.frontend.expenses.components.admin-entity-form :refer [entity-form-modal]]
    [app.domain.frontend.expenses.events.expense-items :as expense-items-events]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]))

(defn- render-add-form
  [{:keys [on-success on-cancel entity-name entity-spec]}]
  ($ entity-form-modal
    {:entity-name entity-name
     :entity-spec entity-spec
     :button-text "Save Expense Item"
     :on-success on-success
     :on-cancel on-cancel}))

(defn- render-edit-form
  [item {:keys [on-success on-cancel entity-name entity-spec]}]
  (let [item-id (id-utils/extract-entity-id item)
        initial-values (assoc item :id item-id)]
    ($ entity-form-modal
      {:entity-name entity-name
       :entity-spec entity-spec
       :editing? true
       :initial-values initial-values
       :button-text "Update Expense Item"
       :on-success on-success
       :on-cancel on-cancel})))

(defui admin-expense-items-page
  []
  (let [refresh-list #(rf/dispatch [::expense-items-events/load-list {}])]
    ($ generic-admin-entity-page
      {:children :expense-items
       :list-overrides {:form-display :modal
                        :render-add-form render-add-form
                        :render-edit-form render-edit-form
                        :on-add-success refresh-list
                        :on-edit-success refresh-list}})))
