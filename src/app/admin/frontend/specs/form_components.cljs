(ns app.admin.frontend.specs.form-components
  "Registry for custom admin form field components.

  Domain modules can register components keyed by entity + field so the
  generic form spec builder can attach them at runtime."
  (:require [taoensso.timbre :as log]))

(defonce ^:private registry (atom {}))

(defn register-form-field-component!
  "Register a custom component for an entity + field.

  Params:
  - :entity-key (keyword)
  - :field-key  (keyword)
  - :component  (fn/component)
  "
  [{:keys [entity-key field-key component]}]
  (when (and entity-key field-key component)
    (swap! registry assoc-in [entity-key field-key] component)
    (log/debug "Registered admin form field component" {:entity entity-key :field field-key})))

(defn get-form-field-component
  "Lookup a registered component for entity + field, or nil."
  [entity-key field-key]
  (get-in @registry [entity-key field-key]))
