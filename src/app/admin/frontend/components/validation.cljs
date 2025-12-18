(ns app.admin.frontend.components.validation
  "Validation utilities for admin frontend components."
  (:require [clojure.string :as str]))

(defn validate-email-format
  "Basic email format validation."
  [email]
  (and (string? email)
    (not (str/blank? email))
    (re-matches #".+@.+\..+" email)))

(defn validate-required-fields
  "Validate that required fields are present and not blank.

   Props:
   - data: Map of data to validate
   - required-fields: Set of required field keys"
  [data required-fields]
  (reduce-kv
    (fn [errors field value]
      (if (or (nil? value)
            (and (string? value) (str/blank? value)))
        (assoc errors field "This field is required")
        errors))
    {}
    (select-keys data required-fields)))
