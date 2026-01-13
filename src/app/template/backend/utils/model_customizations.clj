(ns app.template.backend.utils.model-customizations
  "Utilities for extracting field and entity customizations from models.edn.

   This namespace is part of the backend template layer. Extraction helpers are
   implemented in `app.shared.model-customizations` to avoid duplication/drift.

   This namespace also contains schema-stripping helpers (`strip-*`) that are
   used by migration/alignment tooling to remove UI config from schema sources."
  (:require
    [app.shared.model-customizations :as shared-model-cust]))

;; ---------------------------------------------------------------------------
;; Extraction helpers (delegated to shared)
;; ---------------------------------------------------------------------------

(def extract-field-admin-customizations shared-model-cust/extract-field-admin-customizations)
(def extract-field-form-customizations shared-model-cust/extract-field-form-customizations)
(def extract-field-security-settings shared-model-cust/extract-field-security-settings)

(def extract-computed-fields shared-model-cust/extract-computed-fields)
(def extract-entity-admin-customizations shared-model-cust/extract-entity-admin-customizations)
(def extract-entity-form-customizations shared-model-cust/extract-entity-form-customizations)

(def extract-all-admin-customizations shared-model-cust/extract-all-admin-customizations)

;; Keep these wrapper shapes stable to the historical behavior of this namespace.
;; (The shared versions keywordize entity keys for form/computed extraction.)

(defn extract-all-form-customizations
  "Extract form customizations from complete models data.

   Accepts either a map (EDN) or a vector of pairs (JSON-like) and returns a map
   keyed by the same entity identifiers as provided in the input."
  [models-data]
  (let [md-map (cond
                 (map? models-data) models-data
                 (vector? models-data) (into {} models-data)
                 :else {})]
    (->> md-map
      (map (fn [[entity-name entity-def]]
             [entity-name (extract-entity-form-customizations entity-def)]))
      (into {}))))

(defn extract-all-computed-fields
  "Extract computed field definitions from complete models data.

   Accepts either a map (EDN) or a vector of pairs (JSON-like). Returns only
   entities that have at least one computed field, keyed by the same entity
   identifiers as provided in the input."
  [models-data]
  (let [md-map (cond
                 (map? models-data) models-data
                 (vector? models-data) (into {} models-data)
                 :else {})]
    (->> md-map
      (map (fn [[entity-name entity-def]]
             [entity-name (extract-computed-fields entity-def)]))
      (filter (fn [[_entity-name computed-fields]] (seq computed-fields)))
      (into {}))))

(def role-sufficient? shared-model-cust/role-sufficient?)
(def merge-with-defaults shared-model-cust/merge-with-defaults)
(def filter-by-role shared-model-cust/filter-by-role)
(def filter-by-conditions shared-model-cust/filter-by-conditions)

;; ---------------------------------------------------------------------------
;; Clean separation: Strip admin configuration from models
;; ---------------------------------------------------------------------------

(defn strip-field-admin-config
  "Remove :admin configuration from a field definition.
   This supports the clean separation of UI config from database schema.

   Input:  [:email [:varchar 255] {:admin {:display-order 2 :width '200px'}}]
   Output: [:email [:varchar 255]]

   Input:  [:email [:varchar 255] {:admin {:display-order 2} :null false}]
   Output: [:email [:varchar 255] {:null false}]"
  [field-def]
  (if (and (vector? field-def) (>= (count field-def) 3))
    (let [[field-name field-type constraints] field-def
          cleaned-constraints (dissoc constraints :admin)]
      (if (empty? cleaned-constraints)
        [field-name field-type]
        [field-name field-type cleaned-constraints]))
    field-def))

(defn strip-entity-admin-config
  "Remove all :admin configuration from an entity definition.
   Also removes UI-specific keys like :computed-fields that belong in UI config.

   Input:  {:fields [...] :computed-fields {...} :indexes [...]}
   Output: {:fields [...] :indexes [...]}"
  [entity-def]
  (-> entity-def
      ;; Remove UI-specific top-level keys
    (dissoc :computed-fields :ui-config :admin-config)
      ;; Clean the fields
    (update :fields (fn [fields]
                      (mapv strip-field-admin-config fields)))))

(defn strip-all-admin-config
  "Remove all :admin configuration from complete models data.
   This ensures clean separation between database schema and UI configuration.

   Accepts either a map (EDN) or a vector of pairs (JSON-like)."
  [models-data]
  (let [md-map (cond
                 (map? models-data) models-data
                 (vector? models-data) (into {} models-data)
                 :else {})]
    (->> md-map
      (map (fn [[entity-name entity-def]]
             [entity-name (strip-entity-admin-config entity-def)]))
      (into {}))))
