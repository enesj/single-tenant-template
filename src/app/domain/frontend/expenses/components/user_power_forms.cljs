(ns app.domain.frontend.expenses.components.user-power-forms
  "User-facing modal forms for power-user reference pages (articles, etc.)."
  (:require
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.utils.id :as id-utils]
    [app.shared.adapters.normalization :as normalization]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(def ^:private article-form-spec
  [{:id :canonical_name
    :type :text
    :label "Canonical name"
    :required true
    :placeholder "e.g. Coffee Beans"}])

(defui user-article-add-form-modal
  [{:keys [on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "user-article"
         :entity-spec article-form-spec
         :editing false
         :initial-values {}
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/create-article-modal values on-success]))
         :button-text "Save Article"}))))

(def ^:private article-edit-form-spec
  [{:id :canonical_name
    :type :text
    :label "Canonical name"
    :required true
    :placeholder "e.g. Coffee Beans"}
   {:id :subcategory_id
    :type :select
    :label "Subcategory"
    :required false
    :options ["subcategories" "name"]}
   {:id :manufacturer_id
    :type :select
    :label "Manufacturer"
    :required false
    :options ["manufacturers" "display_name"]}])

(defui user-article-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        ;; Get dynamic form spec from user-settings config
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :articles true])
        item (normalization/convert-db-keys->app-keys item)
        article-id (id-utils/extract-entity-id item)
        manufacturer-id (or (:manufacturer-id item)
                          (:manufacturer_id item)
                          (:manufacturerId item))
        ;; Build initial values from item, covering all possible fields
        initial-values (-> {}
                         (assoc :canonical_name (or (:canonical-name item) (:canonicalName item) ""))
                         (assoc :category (or (:category item) ""))
                         (assoc :subcategory_id (or (some-> (or (:subcategory-id item)
                                                              (:subcategory_id item)
                                                              (:subcategoryId item))
                                                      str)
                                                  ""))
                         (assoc :manufacturer_id (some-> manufacturer-id str))
                         (assoc :id (or (:id item) "")))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "articles"
         ;; Only use hardcoded spec as fallback if dynamic config not available
         :entity-spec (when-not (seq dynamic-spec) article-edit-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-article-modal
                                    (some-> article-id str)
                                    values
                                    on-success]))
         :button-text "Save Article"}))))

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

(def ^:private category-form-spec
  [{:id :name
    :type :text
    :label "Name"
    :required true
    :placeholder "e.g. Beverages"}
   {:id :description
    :type :textarea
    :label "Description"
    :required false
    :placeholder "Optional"}])

(defui user-category-add-form-modal
  [{:keys [on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "categories"
         :entity-spec category-form-spec
         :editing false
         :initial-values {}
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/create-category-modal values on-success]))
         :button-text "Save Category"}))))

(defui user-category-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :categories true])
        item (normalization/convert-db-keys->app-keys item)
        category-id (id-utils/extract-entity-id item)
        initial-values (-> {}
                         (assoc :name (or (:name item) ""))
                         (assoc :description (or (:description item) ""))
                         (assoc :id (or (:id item) "")))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "categories"
         :entity-spec (when-not (seq dynamic-spec) category-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-category-modal
                                    (some-> category-id str)
                                    values
                                    on-success]))
         :button-text "Save Category"}))))

(def ^:private expense-category-form-spec
  [{:id :name
    :type :text
    :label "Name"
    :required true
    :placeholder "e.g. Utilities"}])

(defui user-expense-category-add-form-modal
  [{:keys [on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "expense-categories"
         :entity-spec expense-category-form-spec
         :editing false
         :initial-values {}
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/create-expense-category-modal values on-success]))
         :button-text "Save Expense Category"}))))

(defui user-expense-category-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :expense-categories true])
        item (normalization/convert-db-keys->app-keys item)
        expense-category-id (id-utils/extract-entity-id item)
        initial-values (-> {}
                         (assoc :name (or (:name item) ""))
                         (assoc :id (or (:id item) "")))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "expense-categories"
         :entity-spec (when-not (seq dynamic-spec) expense-category-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-expense-category-modal
                                    (some-> expense-category-id str)
                                    values
                                    on-success]))
         :button-text "Save Expense Category"}))))

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

(def ^:private subcategory-form-spec
  [{:id :category_id
    :type :select
    :label "Category"
    :required true
    :options ["categories" "name"]}
   {:id :name
    :type :text
    :label "Name"
    :required true
    :placeholder "e.g. Coffee"}
   {:id :description
    :type :textarea
    :label "Description"
    :required false
    :placeholder "Optional"}])

(defui user-subcategory-add-form-modal
  [{:keys [on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "subcategories"
         :entity-spec subcategory-form-spec
         :editing false
         :initial-values {}
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/create-subcategory-modal values on-success]))
         :button-text "Save Subcategory"}))))

(defui user-subcategory-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :subcategories true])
        item (normalization/convert-db-keys->app-keys item)
        subcategory-id (id-utils/extract-entity-id item)
        category-id (or (:category-id item)
                      (:category_id item)
                      (:categoryId item))
        initial-values (-> {}
                         (assoc :category_id (some-> category-id str))
                         (assoc :name (or (:name item) ""))
                         (assoc :description (or (:description item) ""))
                         (assoc :id (or (:id item) "")))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "subcategories"
         :entity-spec (when-not (seq dynamic-spec) subcategory-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-subcategory-modal
                                    (some-> subcategory-id str)
                                    values
                                    on-success]))
         :button-text "Save Subcategory"}))))

(def ^:private article-alias-edit-form-spec
  [{:id :raw_label
    :type :text
    :label "Raw label"
    :required true
    :placeholder "e.g. COFFEE BEANS 1KG"}
   {:id :raw_label_normalized
    :type :text
    :label "Normalized label"
    :required true
    :placeholder "e.g. coffee-beans-1kg"}])

(defui user-article-alias-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        ;; Get dynamic form spec from user-settings config
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :article-aliases true])
        item (normalization/convert-db-keys->app-keys item)
        article-alias-id (id-utils/extract-entity-id item)
        ;; Build initial values from item, covering all possible fields
        initial-values (-> {}
                         (assoc :raw_label (or (:raw-label item) ""))
                         (assoc :raw_label_normalized (or (:raw-label-normalized item) ""))
                         (assoc :article_id (or (:article-id item) ""))
                         (assoc :created_at (or (:created-at item) ""))
                         (assoc :supplier_display_name (or (:supplier-display-name item) ""))
                         (assoc :article_canonical_name (or (:article-canonical-name item) ""))
                         (assoc :confidence (or (:confidence item) ""))
                         (assoc :id (or (:id item) ""))
                         (assoc :supplier_id (or (:supplier-id item) "")))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "article-aliases"
         ;; Only use hardcoded spec as fallback if dynamic config not available
         :entity-spec (when-not (seq dynamic-spec) article-alias-edit-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-article-alias-modal
                                    (some-> article-alias-id str)
                                    values
                                    on-success]))
         :button-text "Save Alias"}))))

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


