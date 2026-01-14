(ns app.shared.labels
  "Shared helpers for converting identifiers into human-readable labels.

  Keep these functions tiny and dependency-free so they can be reused across
  specs/validation/UI layers without introducing require cycles."
  (:require
    [clojure.string :as str]))

(defn field-name->label
  "Convert a field identifier into a human readable label.

  Behavior:
  - Strips common foreign-key suffixes in both snake_case and kebab-case (e.g. `_id`, `-id`).
  - Normalizes separators (`_` and `-`) to spaces.
  - Capitalizes the resulting string.

  Intended for UI labels and validation error messages, where we want stable,
  readable defaults when a field doesn't declare an explicit label." 
  [field-name]
  (-> (name field-name)
    ;; Strip common foreign-key suffixes in both snake_case and kebab-case.
    (str/replace #"(_id|-id)$" "")
    ;; Normalize separators.
    (str/replace #"[_-]" " ")
    clojure.string/capitalize))
