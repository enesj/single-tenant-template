(ns app.shared.validation.fork
  "Fork form validation functions.
   This namespace provides validation functions specifically designed
   for use with Fork form library."
  (:require
    [app.shared.validation.builder :as builder]
    [app.shared.validation.core :as core]
    [app.shared.validation.field-types :as field-types]))

(defn validate-single-field
  "Validates a single field and returns an error if invalid.
   Handles type conversion and special cases for different field types."
  [validators entity-name field-id value]
  (let [validation-spec (core/get-field-validator validators entity-name field-id)
        field-type (when validation-spec (first validation-spec))
        ;; Convert value for validation
        converted-value (field-types/convert-field-value validators entity-name field-id value)]

    (when validation-spec
      ;; Handle basic type validations specially to avoid false errors
      (cond
        ;; Skip validation for empty values unless required
        (and (or (nil? value)
               (and (string? value) (empty? value)))
          (core/optional-field? validation-spec))
        nil

        ;; For string inputs - automatically valid if it's a string field and value is a string
        (and (= field-type :string) (string? converted-value))
        nil

        ;; For numeric inputs - automatically valid if it's a number field and value parses as a number
        (and (field-types/numeric-field-type? field-type) (number? converted-value))
        nil

        ;; For all other cases, use the full validation
        :else
        (let [result (core/validation-result validation-spec converted-value)]
          (when-not (:valid? result)
            (:error result)))))))

(defn create-fork-validation
  "Creates a Fork validation function for the given entity and field specs.
   This is a more flexible version that supports dynamic field specifications."
  [validators entity-name entity-spec]
  (fn [values]
    (reduce
      (fn [acc field-spec]
        (let [field-id (keyword (:id field-spec))
              value (get values field-id)]
          ;; Only validate fields that should be validated
          (if (core/should-validate-field? validators entity-name field-id value)
            (if-let [error (validate-single-field validators entity-name field-id value)]
              (assoc acc field-id error)
              acc)
            acc)))
      {}
      entity-spec)))

(defn create-fork-validation-from-models
  "Creates a Fork validation function directly from models data.
   This builds validators on demand and creates the validation function."
  [models-data entity-name entity-spec]
  (let [validators (builder/create-enhanced-validators models-data)]
    (create-fork-validation validators entity-name entity-spec)))
