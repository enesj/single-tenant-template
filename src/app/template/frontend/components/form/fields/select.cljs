(ns app.template.frontend.components.form.fields.select
  "Select field components"
  (:require
   [app.template.frontend.components.common :as common]
   [uix.core :refer [$ defui]]))

(defui select-input
  [{:keys [id label error required inline class _options] :as all-props}]
  (let [base-class "ds-select ds-select-primary"
        label-class "ds-label"
        error-class "text-error"
        props (-> all-props
                (assoc
                  :class (str base-class " " class))
                (dissoc :label :error :inline :input-type :formId :required))]

    ($ :div {:class (str "mb-4" (if inline " flex flex-row items-start gap-4"
                                  " flex flex-col items-start gap-4"))}
      ($ common/label {:text label
                       :class (str label-class (when inline " mb-0 min-w-[150px] text-left"))
                       :for id
                       :required required})
      ($ :div {:class (when inline "flex-1 w-full text-left")}
        ($ common/select props)
        (when error
          ($ :div {:class error-class}
            ($ :div (:message error))))))))
