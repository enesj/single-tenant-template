(ns app.domain.frontend.expenses.components.user-reference-forms
  "User-facing modal forms for reference data (suppliers + payers)."
  (:require
    [app.template.frontend.components.form :refer [form]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(def ^:private supplier-form-spec
  [{:id :display_name
    :type :text
    :label "Display name"
    :required true
    :placeholder "e.g. Amazon"}
   {:id :address
    :type :textarea
    :label "Address"
    :required false
    :placeholder "Optional"}
   {:id :tax_id
    :type :text
    :label "Tax ID"
    :required false
    :placeholder "Optional"}])

(def ^:private payer-type-options
  [{:label "Cash" :value "cash"}
   {:label "Card" :value "card"}
   {:label "Account" :value "account"}
   {:label "Person" :value "person"}])

(def ^:private payer-form-spec
  [{:id :label
    :type :text
    :label "Label"
    :required true
    :placeholder "e.g. Visa 1234"}
   {:id :type
    :type :select
    :label "Type"
    :required true
    :options payer-type-options}
   {:id :is_default
    :type :checkbox
    :label "Default"}])

(defui user-supplier-add-form-modal
  [{:keys [on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "user-supplier"
         :entity-spec supplier-form-spec
         :editing false
         :initial-values {}
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/create-supplier-modal values on-success]))
         :button-text "Save Supplier"}))))

(defui user-supplier-edit-form-modal
  [{:keys [supplier-id initial-data on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        initial-values (select-keys (or initial-data {}) [:display_name :address :tax_id])]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "user-supplier"
         :entity-spec supplier-form-spec
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-supplier-modal supplier-id values on-success]))
         :button-text "Update Supplier"}))))

(defui user-payer-add-form-modal
  [{:keys [on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "user-payer"
         :entity-spec payer-form-spec
         :editing false
         :initial-values {:type "cash" :is_default false}
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/create-payer-modal values on-success]))
         :button-text "Save Payer"}))))

(defui user-payer-edit-form-modal
  [{:keys [payer-id initial-data on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        initial-values (-> (select-keys (or initial-data {}) [:label :type :is_default])
                         (update :is_default #(boolean %)))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "user-payer"
         :entity-spec payer-form-spec
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-payer-modal payer-id values on-success]))
         :button-text "Update Payer"}))))
