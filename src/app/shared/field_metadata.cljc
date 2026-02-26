(ns app.shared.field-metadata
  "Field metadata handling utilities.

   This namespace provides utilities for extracting and working with field
   metadata from models.edn, including field type resolution and model
   introspection functions.

   Functions in this namespace handle:
   - Field type resolution from models
   - Model introspection
   - Entity key normalization
   - Field specification lookup"
  (:require
    [clojure.set]))

;; =============================================================================
;; Entity Key Normalization
;; =============================================================================

(defn normalize-entity-key
  "Normalize entity key to handle both keyword and string inputs.

   Args:
     entity: Entity identifier (keyword or string)

   Returns:
     Normalized keyword entity key"
  [entity]
  (cond
    (keyword? entity) entity
    (string? entity) (keyword entity)
    :else (keyword (str entity))))

(defn- normalize-field-key
  "Normalize field key to handle both keyword and string inputs.

   Args:
     field: Field identifier (keyword or string)

   Returns:
     Normalized keyword field key"
  [field]
  (cond
    (keyword? field) field
    (string? field) (keyword field)
    :else (keyword (str field))))

;; =============================================================================
;; Field Type Resolution
;; =============================================================================

(defn- keywordize-top
  "Normalize top-level model map keys so we can handle JSON (string keys) and EDN (keyword keys)
   without forcing callers to transform the data."
  [models]
  (cond
    (map? models) (into {} (map (fn [[k v]] [(normalize-entity-key k) v]) models))
    (vector? models) (into {} (map (fn [[k v]] [(normalize-entity-key k) v]) models))
    :else {}))

(defn get-field-type
  "Get field type from models data for a specific entity and field.

   Looks up field type in the models schema, checking both :fields and :types
   sections of the entity definition. Accepts models where keys are strings
   (JSON shape) or keywords (EDN)."
  [models entity field]
  (let [models* (keywordize-top models)
        entity-key (normalize-entity-key entity)
        field-key (normalize-field-key field)
        entity-def (get models* entity-key)
        ;; fields/types may contain string field names when loaded from JSON, so
        ;; compare on keywordized first elements
        field-spec (some #(when (= (normalize-field-key (first %)) field-key) %)
                     (:fields entity-def))
        type-spec (when-not field-spec
                    (some #(when (= (normalize-field-key (first %)) field-key) %)
                      (:types entity-def)))]
    (cond
      field-spec (second field-spec)
      type-spec (second type-spec)
      :else nil)))

(defn get-field-spec
  "Get the full field specification from models data.

   Returns the complete field specification including constraints,
   not just the type. Handles both keyword and string keys/field names."
  [models entity field]
  (let [models* (keywordize-top models)
        entity-key (normalize-entity-key entity)
        field-key (normalize-field-key field)]
    (some #(when (= (normalize-field-key (first %)) field-key) %)
      (get-in models* [entity-key :fields]))))

(defn get-field-constraints
  "Get field constraints from models data.

   Extracts constraints like :required, :unique, :min, :max, etc.
   from the field specification.

   Args:
     models: The models data structure from models.edn
     entity: Entity identifier (keyword or string)
     field: Field identifier (keyword or string)

   Returns:
     Map of field constraints or empty map if none found"
  [models entity field]
  (let [field-spec (get-field-spec models entity field)]
    (when field-spec
      (into {} (drop 2 field-spec)))))

;; =============================================================================
;; Model Introspection
;; =============================================================================

(defn get-entity-fields
  "Get all fields for a specific entity.

   Args:
     models: The models data structure from models.edn
     entity: Entity identifier (keyword or string)

   Returns:
     Vector of field specifications for the entity"
  [models entity]
  (let [entity-key (normalize-entity-key entity)]
    (get-in (keywordize-top models) [entity-key :fields] [])))

(defn get-entity-field-names
  "Get all field names for a specific entity.

   Args:
     models: The models data structure from models.edn
     entity: Entity identifier (keyword or string)

   Returns:
     Set of field names (keywords)"
  [models entity]
  (let [fields (get-entity-fields models entity)]
    (set (map first fields))))

(defn get-entity-types
  "Get all type definitions for a specific entity.

   Args:
     models: The models data structure from models.edn
     entity: Entity identifier (keyword or string)

   Returns:
     Vector of type specifications for the entity"
  [models entity]
  (let [entity-key (normalize-entity-key entity)]
    (get-in (keywordize-top models) [entity-key :types] [])))

(defn get-enum-choices
  "Extract enum choices for a field from models.

   Args:
     models: The models data structure from models.edn
     entity: Entity identifier (e.g., :users)
     field: Field identifier (e.g., :role)

   Returns:
     Vector of enum choice strings, or empty vector if not found"
  [models entity field]
  (let [field-type (get-field-type models entity field)]
    (when (and (vector? field-type) (= :enum (keyword (first field-type))))
      (let [enum-type-name (second field-type)
            entity-types (get-entity-types models entity)
            type-def (some #(when (= (normalize-field-key (first %))
                                    (normalize-field-key enum-type-name)) %)
                       entity-types)]
        (get-in type-def [2 :choices] [])))))

;; =============================================================================
;; Field Type Utilities
;; =============================================================================

(defn field-required?
  "Check if a field is required.

   Args:
     models: The models data structure from models.edn
     entity: Entity identifier (keyword or string)
     field: Field identifier (keyword or string)

   Returns:
     Boolean indicating if field is required"
  [models entity field]
  (let [constraints (get-field-constraints models entity field)]
    (boolean (:required constraints))))

(defn field-unique?
  "Check if a field is unique.

   Args:
     models: The models data structure from models.edn
     entity: Entity identifier (keyword or string)
     field: Field identifier (keyword or string)

   Returns:
     Boolean indicating if field is unique"
  [models entity field]
  (let [constraints (get-field-constraints models entity field)]
    (boolean (:unique constraints))))

(defn get-field-default
  "Get the default value for a field.

   Args:
     models: The models data structure from models.edn
     entity: Entity identifier (keyword or string)
     field: Field identifier (keyword or string)

   Returns:
     Default value or nil if none specified"
  [models entity field]
  (let [constraints (get-field-constraints models entity field)]
    (:default constraints)))

;; =============================================================================
;; Validation Utilities
;; =============================================================================

(defn validate-entity-exists
  "Validate that an entity exists in the models data.

   Args:
     models: The models data structure from models.edn
     entity: Entity identifier to validate

   Returns:
     Boolean indicating if entity exists"
  [models entity]
  (let [entity-key (normalize-entity-key entity)]
    (contains? models entity-key)))

;; =============================================================================
;; Debug and Inspection Utilities
;; =============================================================================


