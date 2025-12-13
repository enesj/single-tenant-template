(ns app.domain.frontend.expenses.config.preload
  "Inline (compile-time) expenses domain UI config.

  This mirrors the admin `config/` structure, but is domain-owned and currently
  intended for user-facing pages (static, no remote settings fetch)."
  (:require
    [cljs.reader :as reader]
    [shadow.resource :as resource]))

(defonce ^:private preloaded-entities
  (some-> (resource/inline "app/domain/frontend/expenses/config/entities.edn")
    reader/read-string))

(defonce ^:private preloaded-view-options
  (some-> (resource/inline "app/domain/frontend/expenses/config/view-options.edn")
    reader/read-string))

(defonce ^:private preloaded-form-fields
  (some-> (resource/inline "app/domain/frontend/expenses/config/form-fields.edn")
    reader/read-string))

(defonce ^:private preloaded-table-columns
  (some-> (resource/inline "app/domain/frontend/expenses/config/table-columns.edn")
    reader/read-string))

(defn entity-config
  "Return static entity config for `entity-key` (e.g. `:expenses`)."
  [entity-key]
  (get preloaded-entities entity-key))

(defn view-options
  "Return static view options for `entity-key` (e.g. `:expenses`)."
  [entity-key]
  (get preloaded-view-options entity-key))

(defn form-fields
  "Return static form field config for `entity-key` (e.g. `:expenses`)."
  [entity-key]
  (get preloaded-form-fields entity-key))

(defn table-columns
  "Return static table column config for `entity-key` (e.g. `:expenses`)."
  [entity-key]
  (get preloaded-table-columns entity-key))
