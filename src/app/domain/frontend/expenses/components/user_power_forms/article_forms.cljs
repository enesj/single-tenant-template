(ns app.domain.frontend.expenses.components.user-power-forms.article-forms
  "Article and article-alias modal forms."
  (:require
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.utils.id :as id-utils]
    [app.shared.adapters.normalization :as normalization]
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
