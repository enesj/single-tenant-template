(ns app.domain.frontend.expenses.pages.admin.suppliers
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [app.domain.frontend.expenses.components.admin-entity-form :refer [entity-form-modal]]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]))

(defn- render-add-form
  [{:keys [on-success on-cancel entity-name entity-spec]}]
  ($ entity-form-modal
    {:entity-name entity-name
     :entity-spec entity-spec
     :button-text "Save Supplier"
     :on-success on-success
     :on-cancel on-cancel}))

(defn- render-edit-form
  [item {:keys [on-success on-cancel entity-name entity-spec]}]
  (let [supplier-id (id-utils/extract-entity-id item)
        initial-values (assoc item :id supplier-id)]
    ($ entity-form-modal
      {:entity-name entity-name
       :entity-spec entity-spec
       :editing? true
       :initial-values initial-values
       :button-text "Update Supplier"
       :on-success on-success
       :on-cancel on-cancel})))

(defui admin-suppliers-page []
  (let [refresh-list #(rf/dispatch [::suppliers-events/load-list {}])]
    ($ generic-admin-entity-page
      {:children :suppliers
       :list-overrides {:form-display :modal
                        :render-add-form render-add-form
                        :render-edit-form render-edit-form
                        :on-add-success refresh-list
                        :on-edit-success refresh-list}})))
