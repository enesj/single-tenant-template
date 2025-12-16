(ns app.template.frontend.components.form.fields.checkbox
  "Checkbox input field component for boolean values"
  (:require
    [app.template.frontend.components.common :as common]
    [app.template.frontend.components.form.validation :as validation]
    [uix.core :refer [$ defui]]))

(defui checkbox-input
  [{:keys [id label error required class inline on-change value fork-errors formId] :as all-props}]

  (let [;; Generate ID from formId if not explicitly provided
        field-id (or id (when formId (str formId "-checkbox")))
        error-id (when field-id (str field-id "-error"))
        base-class "ds-checkbox ds-checkbox-primary"
        label-class "ds-label cursor-pointer"
        error-class "text-error"
        ;; Get errors from either the direct error prop or from Fork validation errors
        field-error (or error (when fork-errors (validation/get-field-errors fork-errors (keyword id))))
        ;; Convert value to boolean
        checked? (boolean value)
        props (-> all-props
                (dissoc :form-id :error :input-type :disabled? :validate-server? :inline :fork-errors :value :formId :label :required)
                (assoc
                  :class (str base-class " " class)
                  :type "checkbox"
                  :id field-id
                  :checked checked?
                  :on-change #(on-change (.. % -target -checked))))]

    ($ :div {:class (str "mb-4" (if inline " flex flex-row items-start gap-4"
                                  " flex flex-col items-start gap-4"))}
      (if inline
        ;; Inline layout: checkbox first, then label
        ($ :div {:class "flex items-center gap-2"}
          ($ :input props)
          ($ common/label {:text label
                           :for field-id
                           :required required
                           :class (str label-class " mb-0")}))
        ;; Stacked layout: label first, then checkbox
        ($ :div {:class "flex flex-col gap-2"}
          ($ common/label {:text label
                           :for field-id
                           :required required
                           :class label-class})
          ($ :div {:class "flex items-center"}
            ($ :input props))))
      (when field-error
        ($ :div {:class error-class
                 :id error-id
                 :role "alert"}
          ($ :div (if (string? field-error)
                    field-error
                    (:message field-error))))))))
