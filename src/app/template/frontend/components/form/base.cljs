(ns app.template.frontend.components.form.base
  "Core form component providing form initialization and state management"
  (:require
   [app.shared.validation.fork :as validation-fork]
   [fork.re-frame :as fork]
   [uix.core :refer [$ defui]]
   [uix.re-frame :refer [use-subscribe]]))

(defui initialize-form
  "Initializes form with default values and validation"
  [{:keys [render-fn entity-name entity-spec editing] :as props}]
  (let [;; Create the core validation function without any wrapping
        models-data (use-subscribe [:models-data])
        raw-validation-fn (when (and entity-name models-data)
                            (validation-fork/create-fork-validation-from-models models-data entity-name entity-spec))

        ;; Use a simple approach - we'll only validate when explicitly requested
        form-ready (atom false)

        ;; Create wrapper validation that only activates after data is loaded
        validation-fn (when raw-validation-fn
                        (fn [values]
                          ;; Only validate if ready flag is set or form is being submitted
                          (if @form-ready
                            (raw-validation-fn values)
                            {})))

        ;; Create didMount handler to enable validation after initial render
        component-did-mount-fn (fn [_]
                                 #(reset! form-ready true))

        ;; Use the wrapped validation function
        base-props (cond-> {:validation-mode :blur
                            :server-validation? false
                            :component-did-mount component-did-mount-fn}
                     validation-fn (assoc :validation validation-fn))
        fork-props (merge base-props (dissoc props :render-fn))]

    ($ fork/form
      fork-props
      (fn [form-props]
        ;; Pass along form type info
        (let [enhanced-form-props (cond-> form-props
                                    editing (assoc :editing true)
                                    (not editing) (assoc :adding true))]
          ;; Log form state for debugging
          (when render-fn
            (render-fn enhanced-form-props)))))))
