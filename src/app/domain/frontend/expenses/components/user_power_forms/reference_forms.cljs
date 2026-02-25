(ns app.domain.frontend.expenses.components.user-power-forms.reference-forms
  "Manufacturer, city, store, supplier-alias, and store-alias modal forms."
  (:require
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.utils.id :as id-utils]
    [app.shared.adapters.normalization :as normalization]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

;; ── Manufacturer ─────────────────────────────────────────────────────────────

(def ^:private manufacturer-form-spec
  [{:id :display_name
    :type :text
    :label "Display name"
    :required true
    :placeholder "e.g. Acme Corp"}])

(defui user-manufacturer-add-form-modal
  [{:keys [on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "manufacturers"
         :entity-spec manufacturer-form-spec
         :editing false
         :initial-values {}
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/create-manufacturer-modal values on-success]))
         :button-text "Save Manufacturer"}))))

(def ^:private manufacturer-edit-form-spec
  [{:id :display_name
    :type :text
    :label "Display name"
    :required true
    :placeholder "e.g. Acme Corp"}])

(defui user-manufacturer-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        ;; Get dynamic form spec from user-settings config
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :manufacturers true])
        item (normalization/convert-db-keys->app-keys item)
        manufacturer-id (id-utils/extract-entity-id item)
        initial-values (-> {}
                         (assoc :display_name (or (:display-name item) (:displayName item) ""))
                         (assoc :id (or (:id item) "")))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "manufacturers"
         ;; Only use hardcoded spec as fallback if dynamic config not available
         :entity-spec (when-not (seq dynamic-spec) manufacturer-edit-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-manufacturer-modal
                                    (some-> manufacturer-id str)
                                    values
                                    on-success]))
         :button-text "Save Manufacturer"}))))

;; ── City ─────────────────────────────────────────────────────────────────────

(def ^:private city-form-spec
  [{:id :name
    :type :text
    :label "Name"
    :required true
    :placeholder "e.g. Sarajevo"}])

(defui user-city-add-form-modal
  [{:keys [on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "cities"
         :entity-spec city-form-spec
         :editing false
         :initial-values {}
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/create-city-modal values on-success]))
         :button-text "Save City"}))))

(defui user-city-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :cities true])
        item (normalization/convert-db-keys->app-keys item)
        city-id (id-utils/extract-entity-id item)
        initial-values (-> {}
                         (assoc :name (or (:name item) ""))
                         (assoc :id (or (:id item) "")))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "cities"
         :entity-spec (when-not (seq dynamic-spec) city-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-city-modal
                                    (some-> city-id str)
                                    values
                                    on-success]))
         :button-text "Save City"}))))

;; ── Supplier alias ────────────────────────────────────────────────────────────

(def ^:private supplier-alias-edit-form-spec
  [{:id :raw_label
    :type :text
    :label "Raw label"
    :required true
    :placeholder "e.g. MEGA MARKET"}
   {:id :raw_label_normalized
    :type :text
    :label "Normalized label"
    :required true
    :placeholder "e.g. mega-market"}])

(defui user-supplier-alias-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        ;; Get dynamic form spec from user-settings config
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :supplier-aliases true])
        item (normalization/convert-db-keys->app-keys item)
        supplier-alias-id (id-utils/extract-entity-id item)
        ;; Build initial values from item, covering all possible fields
        initial-values (-> {}
                         (assoc :raw_label (or (:raw-label item) ""))
                         (assoc :raw_label_normalized (or (:raw-label-normalized item) ""))
                         (assoc :supplier_id (or (:supplier-id item) ""))
                         (assoc :confidence (or (:confidence item) ""))
                         (assoc :id (or (:id item) "")))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "supplier-aliases"
         ;; Only use hardcoded spec as fallback if dynamic config not available
         :entity-spec (when-not (seq dynamic-spec) supplier-alias-edit-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-supplier-alias-modal
                                    (some-> supplier-alias-id str)
                                    values
                                    on-success]))
         :button-text "Save Alias"}))))

;; ── Store ─────────────────────────────────────────────────────────────────────

(def ^:private store-add-form-spec
  [{:id :supplier_id
    :type :select
    :label "Supplier"
    :required true
    :options ["suppliers" "display_name"]}
   {:id :display_name
    :type :text
    :label "Display name"
    :required true
    :placeholder "e.g. Mega Market"}
   {:id :address
    :type :textarea
    :label "Address"
    :required false
    :placeholder "Optional"}])

(defui user-store-add-form-modal
  [{:keys [on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "stores"
         :entity-spec store-add-form-spec
         :editing false
         :initial-values {}
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/create-store-modal values on-success]))
         :button-text "Save Store"}))))

(def ^:private store-edit-form-spec
  [{:id :display_name
    :type :text
    :label "Display name"
    :required true
    :placeholder "e.g. Mega Market"}
   {:id :address
    :type :textarea
    :label "Address"
    :required false
    :placeholder "Optional"}])

(defui user-store-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :stores true])
        item (normalization/convert-db-keys->app-keys item)
        store-id (id-utils/extract-entity-id item)
        initial-values (-> {}
                         (assoc :display_name (or (:display-name item) (:displayName item) ""))
                         (assoc :address (or (:address item) ""))
                         (assoc :id (or (:id item) "")))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "stores"
         :entity-spec (when-not (seq dynamic-spec) store-edit-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-store-modal
                                    (some-> store-id str)
                                    values
                                    on-success]))
         :button-text "Save Store"}))))

;; ── Store alias ───────────────────────────────────────────────────────────────

(def ^:private store-alias-edit-form-spec
  [{:id :raw_label
    :type :text
    :label "Raw label"
    :required true
    :placeholder "e.g. MEGA MARKET"}
   {:id :raw_label_normalized
    :type :text
    :label "Normalized label"
    :required true
    :placeholder "e.g. mega-market"}
   {:id :store_id
    :type :select
    :label "Store"
    :required false
    :options ["stores" "display_name"]}
   {:id :confidence
    :type :number
    :label "Confidence"
    :required false
    :step 0.01
    :min 0
    :max 1}])

(defui user-store-alias-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :store-aliases true])
        item (normalization/convert-db-keys->app-keys item)
        store-alias-id (id-utils/extract-entity-id item)
        store-id (or (:store-id item)
                   (:store_id item)
                   (:storeId item))
        initial-values (-> {}
                         (assoc :raw_label (or (:raw-label item) ""))
                         (assoc :raw_label_normalized (or (:raw-label-normalized item) ""))
                         (assoc :store_id (some-> store-id str))
                         (assoc :confidence (or (:confidence item) ""))
                         (assoc :id (or (:id item) "")))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "store-aliases"
         :entity-spec (when-not (seq dynamic-spec) store-alias-edit-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-store-alias-modal
                                    (some-> store-alias-id str)
                                    values
                                    on-success]))
         :button-text "Save Alias"}))))
