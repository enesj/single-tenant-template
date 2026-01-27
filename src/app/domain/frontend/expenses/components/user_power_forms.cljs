(ns app.domain.frontend.expenses.components.user-power-forms
  "User-facing modal forms for power-user reference pages (articles, etc.)."
  (:require
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.utils.id :as id-utils]
    [app.shared.adapters.normalization :as normalization]
    [app.shared.model-naming :as model-naming]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- spec-field-keys
  "Return the set of field keys (as keywords) described by a form spec.

  Handles either a seq of {:id ...} maps or a map keyed by field id." 
  [spec]
  (->> (cond
         (map? spec) (vals spec)
         (sequential? spec) spec
         :else [])
    (keep :id)
    (map keyword)
    set))

(defn- initial-values-for
  "Build initial-values that work with both:

  - enhanced/dynamic specs (kebab-case keys like :display-name)
  - fallback hardcoded specs (snake_case keys like :display_name)

  by merging app keys plus their db-key equivalents." 
  [app-values]
  (merge (model-naming/app-map-keys->db app-values) app-values))

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
                      (rf/dispatch [:user-expenses/create-article-modal
                                    (model-naming/app-map-keys->db values)
                                    on-success]))
         :button-text "Save Article"}))))

(def ^:private article-edit-form-spec
  [{:id :canonical_name
    :type :text
    :label "Canonical name"
    :required true
    :placeholder "e.g. Coffee Beans"}
   {:id :category
    :type :text
    :label "Category"
    :required false
    :placeholder "Optional"}])

(defui user-article-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        ;; Get dynamic form spec from user-settings config
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :articles true])
        spec-to-use (if (seq dynamic-spec) dynamic-spec article-edit-form-spec)
        item (normalization/convert-db-keys->app-keys item)
        article-id (id-utils/extract-entity-id item)
        ;; Build initial values from item.
        ;; NOTE: enhanced specs use kebab-case field ids; fallback specs in this
        ;; file still use snake_case ids. We merge both key styles.
        initial-values (initial-values-for
                         {:canonical-name (or (:canonical-name item) (:canonicalName item) "")
                          :category (or (:category item) "")
                          :manufacturer (or (:manufacturer item) "")
                          :id (or (:id item) "")})]
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
                      (let [payload (select-keys values (spec-field-keys spec-to-use))]
                        (rf/dispatch [:user-expenses/update-article-modal
                                      (some-> article-id str)
                                      (model-naming/app-map-keys->db payload)
                                      on-success])))
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
        spec-to-use (if (seq dynamic-spec) dynamic-spec article-alias-edit-form-spec)
        item (normalization/convert-db-keys->app-keys item)
        article-alias-id (id-utils/extract-entity-id item)
        ;; Build initial values from item.
        initial-values (initial-values-for
                         {:raw-label (or (:raw-label item) "")
                          :raw-label-normalized (or (:raw-label-normalized item) "")
                          :article-id (or (:article-id item) "")
                          :created-at (or (:created-at item) "")
                          :supplier-display-name (or (:supplier-display-name item) "")
                          :article-canonical-name (or (:article-canonical-name item) "")
                          :confidence (or (:confidence item) "")
                          :id (or (:id item) "")
                          :supplier-id (or (:supplier-id item) "")})]
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
                      (let [payload (select-keys values (spec-field-keys spec-to-use))]
                        (rf/dispatch [:user-expenses/update-article-alias-modal
                                      (some-> article-alias-id str)
                                      (model-naming/app-map-keys->db payload)
                                      on-success])))
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
        spec-to-use (if (seq dynamic-spec) dynamic-spec supplier-alias-edit-form-spec)
        item (normalization/convert-db-keys->app-keys item)
        supplier-alias-id (id-utils/extract-entity-id item)
        ;; Build initial values from item.
        initial-values (initial-values-for
                         {:raw-label (or (:raw-label item) "")
                          :raw-label-normalized (or (:raw-label-normalized item) "")
                          :supplier-id (or (:supplier-id item) "")
                          :confidence (or (:confidence item) "")
                          :id (or (:id item) "")})]
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
                      (let [payload (select-keys values (spec-field-keys spec-to-use))]
                        (rf/dispatch [:user-expenses/update-supplier-alias-modal
                                      (some-> supplier-alias-id str)
                                      (model-naming/app-map-keys->db payload)
                                      on-success])))
         :button-text "Save Alias"}))))

(defn- pad-two
  [value]
  (let [s (str value)]
    (if (< (count s) 2) (str "0" s) s)))

(defn- datetime-local
  "Coerce an ISO-ish timestamp into a datetime-local input value (YYYY-MM-DDTHH:MM)."
  [value]
  (let [s (some-> value str)]
    (cond
      (str/blank? s) ""
      (re-matches #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$" s) s
      :else
      (try
        (let [d (js/Date. s)
              t (.getTime d)]
          (if (js/isNaN t)
            ""
            (str (.getFullYear d)
              "-" (pad-two (inc (.getMonth d)))
              "-" (pad-two (.getDate d))
              "T" (pad-two (.getHours d))
              ":" (pad-two (.getMinutes d)))))
        (catch :default _
          "")))))

(def ^:private price-observation-edit-form-spec
  [{:id :observed_at
    :type :timestamp
    :input-type "datetime-local"
    :label "Observed at"
    :required true}
   {:id :qty
    :type :number
    :label "Qty"
    :required true
    :step 0.01
    :min 0}
   {:id :unit_price
    :type :number
    :label "Unit price"
    :required true
    :step 0.01
    :min 0}
   {:id :line_total
    :type :number
    :label "Line total"
    :required false
    :step 0.01
    :min 0}
   {:id :currency
    :type :text
    :label "Currency"
    :required false
    :placeholder "e.g. USD"}])

(defui user-price-observation-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        ;; Get dynamic form spec from user-settings config
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :price-observations true])
        spec-to-use (if (seq dynamic-spec) dynamic-spec price-observation-edit-form-spec)
        item (normalization/convert-db-keys->app-keys item)
        price-observation-id (id-utils/extract-entity-id item)
        observed-at (:observed-at item)
        unit-price (:unit-price item)
        line-total (:line-total item)
        ;; Build initial values from item.
        initial-values (initial-values-for
                         {:observed-at (datetime-local observed-at)
                          :qty (or (:qty item) "")
                          :unit-price (or unit-price "")
                          :line-total (or line-total "")
                          :currency (or (:currency item) "")
                          :id (or (:id item) "")
                          :article-id (or (:article-id item) "")
                          :supplier-id (or (:supplier-id item) "")
                          :expense-item-id (or (:expense-item-id item) "")})]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "price-observations"
         ;; Only use hardcoded spec as fallback if dynamic config not available
         :entity-spec (when-not (seq dynamic-spec) price-observation-edit-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (let [payload (select-keys values (spec-field-keys spec-to-use))]
                        (rf/dispatch [:user-expenses/update-price-observation-modal
                                      (some-> price-observation-id str)
                                      (model-naming/app-map-keys->db payload)
                                      on-success])))
         :button-text "Save Observation"}))))

