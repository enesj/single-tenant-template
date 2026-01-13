(ns app.domain.frontend.expenses.components.user-power-forms
  "User-facing modal forms for power-user reference pages (articles, etc.)."
  (:require
    [app.template.frontend.components.form :refer [form]]
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

