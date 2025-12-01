(ns app.template.frontend.components.form.fields.textarea
  "Textarea field components"
  (:require
    [app.template.frontend.components.common :as common]
    [uix.core :refer [$ defui]]))

(defn clean-props
  "Remove non-HTML props and convert camelCase to lowercase"
  [props]
  (-> props
    (dissoc :label :input-type :validate-server? :success :inputType :formId :required :handle-change :on-change :errors :disabled? :inline)))

(defui textarea-input
  [{:keys [id label placeholder value on-change handle-change errors required disabled? inline] :as props}]
  (let [handle-change (or handle-change
                        (fn [val] (when on-change (on-change val))))
        base-class "ds-textarea"
        label-class "ds-label"
        error-class "text-error"]
    ($ :div {:class (str "mb-4" (when inline " flex items-start gap-4"))}
      ($ common/label {:text label
                       :class (str label-class (when inline " mb-0 min-w-[150px] text-left"))
                       :for id
                       :required required})
      ($ :div {:class (when inline "flex-1 w-full text-left")}
        ($ common/text-area
          (merge
            {:id id
             :value value
             :placeholder placeholder
             :class base-class
             :disabled disabled?
             {:on-change #(handle-change (.. % -target -value))}
             (clean-props props)}))
        (when errors
          ($ :div {:class error-class}
            errors))))))
