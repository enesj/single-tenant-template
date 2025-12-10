(ns app.template.frontend.components.form.fields.number
  "Number input field component for integers and decimals"
  (:require
   [app.template.frontend.components.common :as common]
   [app.template.frontend.components.form.validation :as validation]
   [uix.core :refer [$ defui]]))

(defui number-input
  [{:keys [id label input-type error required class inline _on-change _value fork-errors step] :as all-props}]

  (let [base-class "ds-input ds-input-primary"
        label-class "ds-label"
        error-class "text-error"
        ;; Get errors from either the direct error prop or from Fork validation errors
        field-error (or error (when fork-errors (validation/get-field-errors fork-errors (keyword id))))
        ;; Determine input type and step
        html-input-type (case (or input-type "number")
                          "integer" "number"
                          "decimal" "number"
                          "number")
        input-step (or step
                     (case (or input-type "number")
                       "integer" "1"
                       "decimal" "0.01"
                       "any"))
        props (-> all-props
                (assoc
                  :class (str base-class " " class)
                  :type html-input-type
                  :step input-step)
                (dissoc :error :input-type :disabled? :validate-server? :inline :fork-errors :formId :label :required))]

    ($ :div {:class (str "mb-4" (if inline " flex flex-row items-start gap-4"
                                  " flex flex-col items-start gap-4"))}
      ($ common/label {:text label
                       :for id
                       :required required
                       :class (str label-class (when inline " mb-0 min-w-[150px] text-left"))})
      ($ :div {:class (when inline "flex-1 text-left")}
        ($ common/input props)
        (when field-error
          ($ :div {:class error-class
                   :role "alert"}
            ($ :div (if (string? field-error)
                      field-error
                      (:message field-error)))))))))
