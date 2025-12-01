(ns app.shared.validation.core
  "Core validation functions used across all validation namespaces.
   This namespace provides the fundamental validation operations without
   any platform-specific code or external dependencies."
  (:require
    [malli.core :as m]
    [malli.error :as me]))

(defn validate-value
  "Validates a value against a validator spec.
   Returns the first error message if validation fails, nil otherwise."
  [validator value]
  (when-not (m/validate validator value)
    (let [explain-data (m/explain validator value)
          errors (->> explain-data
                   me/humanize)]
      (first errors))))

(defn validation-result
  "Validates a value against a validator spec.
   Returns a map with :valid? boolean and optional :error message."
  [validator value]
  (if-let [error (validate-value validator value)]
    {:valid? false
     :error error}
    {:valid? true}))

(defn get-field-validator
  "Gets the validation spec for a field from the validators map.
   Takes a validators map, entity name, and field id.
   CRITICAL FIX: Handle mixed key types - entity names are keywords, field names are strings"
  [validators entity-name field-id]
  ;; Convert field-id to string if it's a keyword, since field keys are strings in the validators map
  (let [field-id-str (if (keyword? field-id) (name field-id) field-id)]
    (get-in validators [entity-name field-id-str])))

(defn optional-field?
  "Check if a field is optional (has :maybe marker)"
  [field-spec]
  (and field-spec
    (sequential? field-spec)
    (= :maybe (second field-spec))))

(defn should-validate-field?
  "Determines if a field should be validated based on its value and spec"
  [validators entity-name field-id value]
  (let [spec (get-field-validator validators entity-name field-id)
        optional? (optional-field? spec)]
    (not (and optional?
           (or (nil? value)
             (and (string? value) (empty? value)))))))
