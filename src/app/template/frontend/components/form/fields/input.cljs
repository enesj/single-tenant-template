(ns app.template.frontend.components.form.fields.input
  "Input field components"
  (:require
   [app.template.frontend.components.common :as common]
   [app.template.frontend.components.form.fields.date-picker :refer [date-picker]]
   [app.template.frontend.components.form.validation :as validation]
   [uix.core :refer [$ defui]]))

(def input-width
  {:text "w-1/2"
   :number "w-1/3"
   :date "w-1/3"
   :datetime-local "w-1/3"
   :time "w-1/3"
   :date-time "w-1/3"
   :email "w-1/3"
   :password "w-1/3"
   :search "w-1/3"
   :tel "w-1/3"
   :url "w-1/3"
   :select "w-1/3"
   :textarea "w-full"
   :week "w-1/3"
   :default "w-full"})

(defui input
  [{:keys [id label input-type error required class inline on-change value fork-errors] :as all-props}]

  (let [base-class "ds-input ds-input-primary"
        label-class "ds-label"
        error-class "text-error"
        ;; Get errors from either the direct error prop or from Fork validation errors
        field-error (or error (when fork-errors (validation/get-field-errors fork-errors (keyword id))))
        props (-> all-props
                (assoc
                  :class (str base-class " " class))
                (dissoc :error :input-type :disabled? :validate-server? :inline :fork-errors :formId :label :required))]

    ;; For date type inputs, use the date-picker component
    (if (= (or input-type "text") "date")
      ($ date-picker
        (cond-> all-props
          true (assoc
                 :mode "single"
                 :highlighted-dates #{}                     ;; Empty set to avoid test data
                 :highlighted-class "ds-bg-secondary"       ;; Using DaisyUI class
                 :selected-date value                       ;; Only use value, don't provide default
                 :on-date-change #(on-change %))))

      ;; For all other input types, use the existing implementation
      ($ :div {:class (str "mb-4" (if inline " flex flex-row items-start gap-4"
                                    " flex flex-col items-start gap-4"))}
        ($ common/label {:text label
                         :for id
                         :required required
                         :class (str label-class (when inline " mb-0 min-w-[150px] text-left"))})
        ($ :div {:class (when inline "flex-1 text-left")}
          ($ common/input
            (cond-> props
              (:input-type all-props)
              (assoc :type (or input-type "text"))
              (:step all-props)
              (assoc :step (or (:step all-props) "0.01"))))
          (when field-error
            ($ :div {:class error-class
                     :role "alert"}
              ($ :div (if (string? field-error)
                        field-error
                        (:message field-error))))))))))
