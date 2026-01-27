(ns app.domain.frontend.expenses.components.user-reference-forms
  "User-facing modal forms for reference data (suppliers + payers)."
  (:require
    [app.shared.adapters.normalization :as norm]
    [app.shared.model-naming :as model-naming]
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
    :placeholder "Optional"}])

(defn- payer-form-spec
  [payer-type-options]
  [{:id :label
    :type :text
    :label "Label"
    :required true
    :placeholder "e.g. Visa 1234"}
   {:id :payer_type_id
    :type :select
    :label "Type"
    :required true
    :options payer-type-options}
   {:id :is_default
    :type :checkbox
    :label "Default"}])

(def ^:private payer-type-form-spec
  [{:id :label
    :type :text
    :label "Label"
    :required true
    :placeholder "e.g. Family"}
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
                      (rf/dispatch [:user-expenses/create-supplier-modal values (when on-success (fn [& _] (on-success)))]))
         :button-text "Save Supplier"}))))

(defui user-supplier-edit-form-modal
  [{:keys [supplier-id initial-data on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        ;; Get dynamic form spec from user-settings config
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :suppliers true])
        ;; Build initial values from data, covering all possible fields.
        ;; The dynamic form spec uses kebab-case IDs, but our entities may be snake_case.
        initial-values (-> (norm/convert-db-keys->app-keys (or initial-data {}))
                         (select-keys [:display-name :address :id :normalized-key :archived-at]))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "suppliers"
         ;; Only use hardcoded spec as fallback if dynamic config not available
         :entity-spec (when-not (seq dynamic-spec) supplier-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      ;; Convert kebab-case form keys to snake_case for the API.
                      (rf/dispatch [:user-expenses/update-supplier-modal
                                    supplier-id
                                    (model-naming/app-map-keys->db values)
                                    on-success]))
         :button-text "Update Supplier"}))))

(defui user-payer-add-form-modal
  [{:keys [on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        payer-types (or (use-subscribe [:user-expenses/payer-types]) [])
        payer-type-options (mapv (fn [{:keys [id label]}]
                                   {:label label :value (str id)})
                             payer-types)
        default-type-id (or (some->> payer-types
                              (some (fn [pt]
                                      (when (true? (:is_default pt))
                                        (:id pt))))
                              str)
                          (get-in payer-type-options [0 :value]))
        initial-values (cond-> {:is_default false}
                         default-type-id (assoc :payer_type_id default-type-id))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "user-payer"
         :entity-spec (payer-form-spec payer-type-options)
         :editing false
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/create-payer-modal values on-success]))
         :button-text "Save Payer"}))))

(defui user-payer-edit-form-modal
  [{:keys [payer-id initial-data on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        ;; Get dynamic form spec from user-settings config
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :payers true])
        payer-types (or (use-subscribe [:user-expenses/payer-types]) [])
        payer-type-options (mapv (fn [{:keys [id label]}]
                                   {:label label :value (str id)})
                             payer-types)
        default-type-id (or (some->> payer-types
                              (some (fn [pt]
                                      (when (true? (:is_default pt))
                                        (:id pt))))
                              str)
                          (get-in payer-type-options [0 :value]))
        ;; Build initial values from data.
        ;; Dynamic form spec uses kebab-case IDs (e.g. :payer-type-id, :is-default).
        ;; The entity data often uses snake_case keys.
        initial-values (-> (norm/convert-db-keys->app-keys (or initial-data {}))
             (select-keys [:label :payer-type-id :is-default :id :last4])
             (update :payer-type-id #(or (when % (str %)) default-type-id))
             (update :is-default boolean))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "payers"
         ;; Only use hardcoded spec as fallback if dynamic config not available
         :entity-spec (when-not (seq dynamic-spec) (payer-form-spec payer-type-options))
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      ;; Convert kebab-case form keys to snake_case for the API.
                      (rf/dispatch [:user-expenses/update-payer-modal
                                    payer-id
                                    (model-naming/app-map-keys->db values)
                                    on-success]))
         :button-text "Update Payer"}))))

(defui user-payer-type-add-form-modal
  [{:keys [on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "user-payer-type"
         :entity-spec payer-type-form-spec
         :editing false
         :initial-values {:is_default false}
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/create-payer-type-modal values on-success]))
         :button-text "Save Payer Type"}))))

(defui user-payer-type-edit-form-modal
  [{:keys [payer-type-id initial-data on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        ;; Get dynamic form spec from user-settings config
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :payer-types true])
        ;; Build initial values from data, covering all possible fields
        initial-values (-> (select-keys (or initial-data {}) [:label :is_default :id])
                         (update :is_default #(boolean %)))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "payer-types"
         ;; Only use hardcoded spec as fallback if dynamic config not available
         :entity-spec (when-not (seq dynamic-spec) payer-type-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-payer-type-modal payer-type-id values on-success]))
         :button-text "Update Payer Type"}))))
