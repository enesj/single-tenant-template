(ns app.domain.frontend.expenses.components.user-power-forms
  "User-facing modal forms for power-user reference pages (articles, etc.)."
  (:require
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.utils.id :as id-utils]
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
   {:id :barcode
    :type :text
    :label "Barcode"
    :required false
    :placeholder "Optional"}
   {:id :category
    :type :text
    :label "Category"
    :required false
    :placeholder "Optional"}])

(defui user-article-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        article-id (id-utils/extract-entity-id item)
        initial-values {:canonical_name (or (:canonical_name item)
                                          (:canonical-name item)
                                          (:canonicalName item)
                                          "")
                        :barcode (or (:barcode item) "")
                        :category (or (:category item) "")}]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "user-article"
         :entity-spec article-edit-form-spec
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
  [{:id :raw_label_normalized
    :type :text
    :label "Alias"
    :required true
    :placeholder "e.g. COFFEE BEANS 1KG"}
   {:id :confidence
    :type :number
    :label "Confidence"
    :required false
    :step 0.01
    :min 0
    :max 1}])

(defui user-article-alias-edit-form-modal
  [{:keys [item on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        article-alias-id (id-utils/extract-entity-id item)
        initial-values {:raw_label_normalized (or (:raw_label_normalized item)
                                                (:raw-label-normalized item)
                                                "")
                        :confidence (or (:confidence item) "")}]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "user-article-alias"
         :entity-spec article-alias-edit-form-spec
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-article-alias-modal
                                    (some-> article-alias-id str)
                                    values
                                    on-success]))
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
        price-observation-id (id-utils/extract-entity-id item)
        observed-at (or (:observed_at item) (:observed-at item))
        unit-price (or (:unit_price item) (:unit-price item))
        line-total (or (:line_total item) (:line-total item))
        initial-values {:observed_at (datetime-local observed-at)
                        :qty (or (:qty item) "")
                        :unit_price (or unit-price "")
                        :line_total (or line-total "")
                        :currency (or (:currency item) "")}]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "user-price-observation"
         :entity-spec price-observation-edit-form-spec
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (rf/dispatch [:user-expenses/update-price-observation-modal
                                    (some-> price-observation-id str)
                                    values
                                    on-success]))
         :button-text "Save Observation"}))))

