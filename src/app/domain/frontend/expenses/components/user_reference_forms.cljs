(ns app.domain.frontend.expenses.components.user-reference-forms
  "User-facing modal forms for reference data (suppliers + payers)."
  (:require
    [app.shared.adapters.normalization :as norm]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.i18n :refer [use-t]]
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

(defn- payer-label-field
  [t placeholder-key]
  {:id :label
   :type :text
   :label (t :common/label)
   :required true
   :placeholder (t placeholder-key)})

(defn- payer-default-field
  [t field-id]
  {:id field-id
   :type :checkbox
   :label (t :common/is-default)})

(defn- payer-active-field
  [t]
  {:id :is-active
   :type :checkbox
   :label (t :common/active)})

(defn- payer-form-spec
  [t]
  [(payer-label-field t :payers/form-placeholder)
   (payer-default-field t :is_default)])

(defn- payer-label-only-form-spec
  [t]
  [(payer-label-field t :payers/form-own-placeholder)])

(defn- payer-label-and-default-form-spec
  [t]
  [(payer-label-field t :payers/form-own-placeholder)
   (payer-default-field t :is-default)])

(defn- payer-full-form-spec
  [t]
  [(payer-label-field t :payers/form-placeholder)
   (payer-default-field t :is-default)
   (payer-active-field t)])

(defn- payer-edit-form-spec
  [t edit-mode]
  (case edit-mode
    :label-only (payer-label-only-form-spec t)
    :label-and-default (payer-label-and-default-form-spec t)
    (payer-full-form-spec t)))

(defn- payer-edit-initial-values
  [edit-mode initial-data]
  (let [normalized-data (norm/convert-db-keys->app-keys (or initial-data {}))]
    (case edit-mode
      :label-only {:label (or (:label normalized-data) "")}
      :label-and-default {:label (or (:label normalized-data) "")
                          :is-default (boolean (:is-default normalized-data))}
      {:label (or (:label normalized-data) "")
       :is-default (boolean (:is-default normalized-data))
       :is-active (if (contains? normalized-data :is-active)
                    (boolean (:is-active normalized-data))
                    true)})))

(defn- payer-edit-submit-data
  [edit-mode values]
  (case edit-mode
    :label-only {:label (:label values)}
    :label-and-default (-> values
                         (select-keys [:label :is-default])
                         model-naming/app-map-keys->db)
    (-> values
      (select-keys [:label :is-default :is-active])
      model-naming/app-map-keys->db)))

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
                         (select-keys [:display-name :address :id :normalized-key]))]
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
        t (use-t)]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "user-payer"
         :entity-spec (payer-form-spec t)
         :editing false
         :initial-values {:is_default false}
         :legend (t :payers/form-add-title)
         :success-message (t :payers/form-created-success)
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/create-payer-modal values on-success]))
         :button-text (t :common/save)}))))

(defui user-payer-edit-form-modal
  [{:keys [payer-id initial-data on-success on-cancel edit-mode]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        t (use-t)
        edit-mode (or edit-mode :full)
        initial-values (payer-edit-initial-values edit-mode initial-data)]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        ;; Use a non-registry entity-name so the shared form component does not
        ;; pick up the dynamic models-derived :payers spec (which would render
        ;; an editable type dropdown). The static spec below governs the form.
        {:entity-name "user-payer-edit"
         ;; Intentionally fixed for user-facing payer edits: type is never editable here.
         :entity-spec (payer-edit-form-spec t edit-mode)
         :editing true
         :initial-values initial-values
         :legend (t :payers/form-edit-title)
         :success-message (t :payers/form-updated-success)
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-payer-modal
                                    payer-id
                                    (payer-edit-submit-data edit-mode values)
                                    on-success]))
         :button-text (t :common/save-changes)}))))
