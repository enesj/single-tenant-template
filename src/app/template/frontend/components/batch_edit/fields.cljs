(ns app.template.frontend.components.batch-edit.fields
  (:require
    [app.template.frontend.components.batch-edit.field-props :as field-props]
    [app.template.frontend.components.form.fields.array-input :refer [array-input]]
    [app.template.frontend.components.form.fields.checkbox :refer [checkbox-input]]
    [app.template.frontend.components.form.fields.input :refer [input]]
    [app.template.frontend.components.form.fields.json-editor :refer [json-editor]]
    [app.template.frontend.components.form.fields.number :refer [number-input]]
    [app.template.frontend.components.form.fields.select :refer [select-input]]
    [app.template.frontend.components.form.fields.textarea :refer [textarea-input]]
    [uix.core :as uix :refer [$ defui]]))

(defmulti render-field-by-type
  "Multimethod to render different field types for batch editing"
  (fn [component-type _field-props] component-type))

(defmethod render-field-by-type :input
  [_ field-props]
  ($ input field-props))

(defmethod render-field-by-type :number-input
  [_ field-props]
  ($ number-input field-props))

(defmethod render-field-by-type :checkbox-input
  [_ field-props]
  ($ checkbox-input field-props))

(defmethod render-field-by-type :textarea-input
  [_ field-props]
  ($ textarea-input field-props))

(defmethod render-field-by-type :select-input
  [_ field-props]
  ($ select-input field-props))

(defmethod render-field-by-type :json-editor
  [_ field-props]
  ($ json-editor field-props))

(defmethod render-field-by-type :array-input
  [_ field-props]
  ($ array-input field-props))

;; Default method for unrecognized field types
(defmethod render-field-by-type :default
  [component-type field-props]
  (js/console.warn "Unknown field component type:" component-type)
  ($ input field-props))

(defui render-batch-field-component
  "Renders a batch edit field component by determining the correct type and props"
  [{:keys [field-spec entity-name errors form-id values validators set-values set-form-values set-dirty-fields error]}]
  ;; Early return if field-spec is nil to prevent errors
  (when-not field-spec
    ($ :div {:class "error-field"} "Field spec missing"))

  (let [;; Convert field-spec back from JS to ClojureScript if needed
        field-spec-clj (cond
                         (nil? field-spec)
                         nil

                         (object? field-spec)
                         (js->clj field-spec :keywordize-keys true)

                         :else
                         field-spec)

        field-type (:type field-spec-clj)
        input-type (:input-type field-spec-clj)
        field-id (:id field-spec-clj)

        component-type (field-props/determine-field-component field-type input-type field-id)

        field-props-map (field-props/build-field-props
                          {:field-spec field-spec-clj
                           :entity-name entity-name
                           :values values
                           :errors errors
                           :form-id form-id
                           :validators validators
                           :set-values set-values
                           :set-form-values set-form-values
                           :set-dirty-fields set-dirty-fields
                           :error error})]

    (render-field-by-type component-type field-props-map)))
