(ns app.domain.frontend.expenses.components.user-power-forms
  "User-facing modal forms for power-user reference pages (articles, etc.)."
  (:require
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.utils.id :as id-utils]
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

