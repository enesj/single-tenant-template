(ns app.template.frontend.components.batch-edit.field-props
  (:require
    [app.shared.validation.core :as validation-core]
    [app.shared.validation.fork :as validation-fork]
    [app.template.frontend.components.form.fields.input :refer [input-width]]
    [app.template.frontend.events.form :as form-events]
    [re-frame.core :as rf]))

(defn create-change-handler
  "Creates a change handler for batch edit fields with validation.
   Uses single-field updates to avoid stale closure issues with fork's merge-based set-values."
  [{:keys [field-key entity-name set-values set-form-values set-dirty-fields validators]}]
  (fn [e]
    (let [target-value (cond
                         ;; Handle date picker events (e is directly the value)
                         (not (.-target e)) e
                         ;; Handle regular input events
                         :else (.. e -target -value))]

      ;; Track dirty fields in local state
      (when set-dirty-fields
        (set-dirty-fields (fn [prev] (conj prev field-key))))

      ;; Clear any form-level errors when user makes changes
      (rf/dispatch [::form-events/clear-form-errors entity-name])

      ;; Perform validation if needed
      (when (and validators (validation-core/should-validate-field? validators entity-name field-key target-value))
        ;; Check if there's an error
        (if-let [validation-error (when validators
                                    (validation-fork/validate-single-field validators entity-name field-key target-value))]
          ;; Set validation error
          (rf/dispatch [::form-events/set-field-error
                        entity-name
                        field-key
                        {:message validation-error}])
          ;; Clear validation error if no error
          (rf/dispatch [::form-events/clear-field-error entity-name field-key])))

      ;; Update fork values with only the changed field to avoid stale closure overwriting other fields
      (when set-values
        (set-values {field-key target-value})
        ;; Also update component state if available
        (when set-form-values
          (set-form-values (fn [prev] (assoc prev field-key target-value))))))))

(defn build-field-props
  "Builds the props map for a batch edit field"
  [{:keys [field-spec entity-name values _errors _form-id validators set-values set-form-values set-dirty-fields error]}]
  (if (nil? field-spec)
    {} ; Return empty props to prevent crash

    (let [field-id (:id field-spec)
          field-key (keyword field-id)
          field-value (get values field-key)
          is-unique-field? (= (:validate-server? field-spec) "unique")
          is-select? (= (:input-type field-spec) "select")
          is-mixed-value? (nil? field-value)
          change-handler (create-change-handler
                           {:field-key field-key
                            :entity-name entity-name
                            :set-values set-values
                            :set-form-values set-form-values
                            :set-dirty-fields set-dirty-fields
                            :validators validators})
          base-props (merge
                       (select-keys field-spec [:id :type :input-type :options :label :step])
                       {:disabled is-unique-field?           ;; Disable unique fields
                        :required false                      ;; No field is required in batch edit
                        :value field-value                   ;; Direct from values, not nested
                        :mixed-value? is-mixed-value?
                        :class ((keyword (:input-type field-spec)) input-width)
                        :error error
                        :data-xss-protection true
                        :success nil
                        :inline true
                        :auto-complete "on"
                        :on-change change-handler             ;; Use direct handler for changes
                        :aria-label (str "Enter " (:label field-spec))
                        :aria-required false                 ;; No field is required
                        :aria-invalid (boolean error)
                        :validate-server? false})]

      ;; Add props specific to field types
      (cond-> base-props
        ;; For select fields, add mixed values support
        is-select?
        (assoc :show-mixed-values? is-mixed-value?)

        ;; For non-select fields, add placeholder for mixed values
        (and is-mixed-value? (not is-select?))
        (assoc :placeholder "(Mixed values)")))))          ;; Disable server validation for batch edit

(defn determine-field-component
  "Determines which component to use for a given field type and input type.

  Batch edit field specs can come from multiple sources (EDN config, JSON, or
  JS objects), so `:type` / `:input-type` may arrive as keywords or strings.
  This function normalizes both and mirrors the logic used by the main form
  renderer so select/textarea/etc render correctly in batch edit."
  [field-type input-type _field-id]
  (let [->kw (fn [x]
               (cond
                 (keyword? x) x
                 (string? x) (keyword x)
                 (symbol? x) (keyword (name x))
                 :else nil))
        ft (->kw field-type)
        it (->kw input-type)
        ui-type (or (when (#{:input :number :checkbox :textarea :select :json :array} ft)
                      ft)
                  (cond
                    (#{:string :text :varchar :uuid :timestamp :date :datetime :datetime-local} ft) :input
                    (#{:integer :decimal :bigint :float :double :number} ft) :number
                    (#{:boolean} ft) :checkbox
                    (#{:json :jsonb} ft) :json
                    (#{:array} ft) :array
                    :else nil))]
    (case ui-type
      :input :input
      :number :number-input
      :checkbox :checkbox-input
      :textarea :textarea-input
      :select :select-input
      :json :json-editor
      :array :array-input
      ;; If `:type` is missing/unknown, infer from `:input-type`.
      (cond
        (#{:select} it) :select-input
        (#{:integer :decimal :bigint :float :double :number} it) :number-input
        (#{:boolean} it) :checkbox-input
        (#{:jsonb :json} it) :json-editor
        (#{:array} it) :array-input
        :else :input))))
