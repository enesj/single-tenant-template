(ns app.shared.validation.unique
  "Unique value validation with platform-specific implementations.
   This namespace provides validation for unique constraints without
   direct database access - instead accepting functions to retrieve values.")

(defn create-unique-validator-with-context
  "Creates a unique validator with entity and field context.
   The get-values-fn should accept entity-type and field-name as parameters."
  [entity-type field-name get-values-fn]
  [:fn {:error/message "This value already exists"}
   (fn [value]
     (let [existing-values (get-values-fn entity-type field-name)]
       (not (contains? (set existing-values) value))))])

;; Platform-specific value retrieval functions
;; These should be injected by the calling code rather than defined here


