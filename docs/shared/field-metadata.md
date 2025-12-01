<!-- ai: {:tags [:shared :data] :kind :reference} -->

# Field Metadata Documentation

The field metadata system provides comprehensive utilities for introspecting and working with entity field definitions, enabling dynamic form generation, validation, and database operations.

## 🏗️ Overview

The field metadata module extracts and normalizes field information from the models schema, providing a consistent interface for accessing field types, constraints, and relationships.

**Location**: `src/app/shared/field_metadata.cljc`

## 🎯 Core Functions

### Entity and Field Normalization

```clojure
(ns app.shared.field-metadata)

(defn normalize-entity-key
  "Normalize entity key to handle both keyword and string inputs"
  [entity]
  ;; Returns normalized keyword)

(defn normalize-field-name
  "Normalize field name to consistent format"
  [field-name]
  ;; Returns normalized keyword)

(defn get-entity-fields
  "Get all fields for an entity"
  [models entity-key]
  ;; Returns map of field definitions)
```

### Field Type Information

```clojure
(defn get-field-type
  "Get the type of a specific field"
  [models entity-key field-name]
  ;; Returns field type keyword)

(defn get-field-spec
  "Get complete field specification"
  [models entity-key field-name]
  ;; Returns field specification map)

(defn is-required-field?
  "Check if field is required"
  [models entity-key field-name]
  ;; Returns boolean)

(defn is-reference-field?
  "Check if field is a reference to another entity"
  [models entity-key field-name]
  ;; Returns boolean)
```

### Field Relationships

```clojure
(defn get-reference-entity
  "Get the entity that a reference field points to"
  [models entity-key field-name]
  ;; Returns referenced entity keyword)

(defn get-foreign-key-fields
  "Get all foreign key fields for an entity"
  [models entity-key]
  ;; Returns map of foreign key fields)

(defn get-reverse-references
  "Get entities that reference this entity"
  [models entity-key]
  ;; Returns list of referencing entities)
```

## 📊 Field Type Detection

### Supported Field Types

The system recognizes and handles these field types:

| Type | Description | Example |
|------|-------------|---------|
| `:text` | String values | `"Property Name"` |
| `:integer` | Integer numbers | `42` |
| `:decimal` | Decimal numbers | `123.45` |
| `:boolean` | True/false values | `true` |
| `:jsonb` | JSON objects | `{:settings "value"}` |
| `:timestamptz` | Timestamps with timezone | `"2025-07-15T10:00:00Z"` |
| `:uuid` | UUID values | `#uuid "550e8400-e29b-41d4-a716-446655440000"` |
| `:enum` | Enumerated values | `"active"` |
| `:reference` | Foreign key references | `123` |

### Type Detection Examples

```clojure
(require '[app.shared.field-metadata :as field-meta])

;; Get field type
(field-meta/get-field-type models-data :properties :name)
;; => :text

(field-meta/get-field-type models-data :properties :owner_id)
;; => :reference

(field-meta/get-field-type models-data :properties :settings)
;; => :jsonb

(field-meta/get-field-type models-data :properties :created_at)
;; => :timestamptz
```

## 🔧 Usage Examples

### Basic Field Information

```clojure
(require '[app.shared.field-metadata :as field-meta])

;; Normalize entity key
(field-meta/normalize-entity-key "properties")
;; => :properties

(field-meta/normalize-entity-key :properties)
;; => :properties

;; Get all fields for entity
(field-meta/get-entity-fields models-data :properties)
;; => {:id :uuid
;;     :name :text
;;     :owner_id :reference
;;     :settings :jsonb
;;     :created_at :timestamptz
;;     ...}

;; Get specific field type
(field-meta/get-field-type models-data :properties :name)
;; => :text
```

### Field Validation Information

```clojure
;; Check if field is required
(field-meta/is-required-field? models-data :properties :name)
;; => true

(field-meta/is-required-field? models-data :properties :description)
;; => false

;; Check if field is a reference
(field-meta/is-reference-field? models-data :properties :owner_id)
;; => true

(field-meta/is-reference-field? models-data :properties :name)
;; => false
```

### Reference Field Information

```clojure
;; Get referenced entity
(field-meta/get-reference-entity models-data :properties :owner_id)
;; => :users

(field-meta/get-reference-entity models-data :transactions :property_id)
;; => :properties

;; Get all foreign key fields
(field-meta/get-foreign-key-fields models-data :properties)
;; => {:owner_id :users}

;; Get reverse references
(field-meta/get-reverse-references models-data :users)
;; => [:properties :transactions :invitations]
```

## 🎨 Integration with Forms

### Dynamic Form Generation

```clojure
(defn generate-form-fields [models-data entity-key]
  (let [fields (field-meta/get-entity-fields models-data entity-key)]
    (for [[field-name field-type] fields]
      {:field-name field-name
       :field-type field-type
       :required? (field-meta/is-required-field? models-data entity-key field-name)
       :reference? (field-meta/is-reference-field? models-data entity-key field-name)
       :reference-entity (when (field-meta/is-reference-field? models-data entity-key field-name)
                          (field-meta/get-reference-entity models-data entity-key field-name))})))

;; Usage
(generate-form-fields models-data :properties)
;; => [{:field-name :name
;;      :field-type :text
;;      :required? true
;;      :reference? false}
;;     {:field-name :owner_id
;;      :field-type :reference
;;      :required? true
;;      :reference? true
;;      :reference-entity :users}
;;     ...]
```

### Form Field Component Selection

```clojure
(defn select-field-component [models-data entity-key field-name]
  (let [field-type (field-meta/get-field-type models-data entity-key field-name)]
    (case field-type
      :text :text-input
      :integer :number-input
      :decimal :number-input
      :boolean :checkbox
      :jsonb :json-editor
      :timestamptz :datetime-picker
      :uuid :uuid-input
      :enum :select-dropdown
      :reference :reference-selector
      :text-input))) ; default

;; Usage
(select-field-component models-data :properties :name)
;; => :text-input

(select-field-component models-data :properties :owner_id)
;; => :reference-selector
```

## 🔍 Validation Integration

### Field Validation Setup

```clojure
(require '[app.shared.validation.field-types :as field-types])

(defn create-field-validator [models-data entity-key field-name]
  (let [field-type (field-meta/get-field-type models-data entity-key field-name)
        required? (field-meta/is-required-field? models-data entity-key field-name)]
    (cond
      (= field-type :text) (field-types/string-validator required?)
      (= field-type :integer) (field-types/numeric-validator required?)
      (= field-type :jsonb) (field-types/json-validator required?)
      (field-meta/is-reference-field? models-data entity-key field-name)
      (field-types/reference-validator
        (field-meta/get-reference-entity models-data entity-key field-name)
        required?)
      :else (field-types/default-validator required?))))

;; Usage
(let [validator (create-field-validator models-data :properties :name)]
  (validator "Property Name"))
;; => {:valid? true :error nil}
```

## 🗄️ Database Integration

### Query Building

```clojure
(defn build-select-query [models-data entity-key fields]
  (let [table-name (field-meta/normalize-entity-key entity-key)
        field-specs (field-meta/get-entity-fields models-data entity-key)]
    {:select (or fields (keys field-specs))
     :from [table-name]}))

;; Usage
(build-select-query models-data :properties [:name :owner_id])
;; => {:select [:name :owner_id] :from [:properties]}
```

### Join Query Building

```clojure
(defn build-join-query [models-data entity-key include-references?]
  (let [base-query (build-select-query models-data entity-key nil)]
    (if include-references?
      (let [fk-fields (field-meta/get-foreign-key-fields models-data entity-key)]
        (reduce
          (fn [query [field-name ref-entity]]
            (update query :left-join conj
              [ref-entity [:= (keyword (str (name entity-key) "." (name field-name)))
                             (keyword (str (name ref-entity) ".id"))]]))
          base-query
          fk-fields))
      base-query)))

;; Usage
(build-join-query models-data :properties true)
;; => {:select [...]
;;     :from [:properties]
;;     :left-join [[:users [:= :properties.owner_id :users.id]]]}
```

## 🎯 Performance Considerations

### Caching Field Information

```clojure
(def field-cache (atom {}))

(defn cached-get-field-type [models-data entity-key field-name]
  (let [cache-key [entity-key field-name]]
    (if-let [cached-type (get @field-cache cache-key)]
      cached-type
      (let [field-type (field-meta/get-field-type models-data entity-key field-name)]
        (swap! field-cache assoc cache-key field-type)
        field-type))))
```

### Batch Operations

```clojure
(defn get-multiple-field-types [models-data entity-key field-names]
  (let [fields (field-meta/get-entity-fields models-data entity-key)]
    (select-keys fields field-names)))

;; Usage
(get-multiple-field-types models-data :properties [:name :owner_id :settings])
;; => {:name :text :owner_id :reference :settings :jsonb}
```

## 🧪 Testing

### Unit Tests

```clojure
(deftest test-normalize-entity-key
  (testing "string normalization"
    (is (= :properties (field-meta/normalize-entity-key "properties"))))

  (testing "keyword passthrough"
    (is (= :properties (field-meta/normalize-entity-key :properties))))

  (testing "invalid input"
    (is (= :invalid (field-meta/normalize-entity-key 123)))))

(deftest test-get-field-type
  (testing "text field"
    (is (= :text (field-meta/get-field-type models-data :properties :name))))

  (testing "reference field"
    (is (= :reference (field-meta/get-field-type models-data :properties :owner_id))))

  (testing "jsonb field"
    (is (= :jsonb (field-meta/get-field-type models-data :properties :settings)))))
```

### Integration Tests

```clojure
(deftest test-field-metadata-integration
  (testing "form field generation"
    (let [form-fields (generate-form-fields models-data :properties)]
      (is (seq form-fields))
      (is (every? #(contains? % :field-name) form-fields))
      (is (every? #(contains? % :field-type) form-fields))))

  (testing "validation integration"
    (let [validator (create-field-validator models-data :properties :name)]
      (is (function? validator))
      (is (:valid? (validator "Test Property"))))))
```

## 🔄 Migration from Legacy

### Before (Scattered Field Logic)

```clojure
;; Field information was scattered across multiple files
(get-in models [entity :fields field])
(contains? required-fields field)
(get reference-map field)
```

### After (Centralized Field Metadata)

```clojure
;; All field operations centralized
(require '[app.shared.field-metadata :as field-meta])

(field-meta/get-field-type models entity field)
(field-meta/is-required-field? models entity field)
(field-meta/get-reference-entity models entity field)
```

### Key Improvements

1. **Consistent Interface**: All field operations through single namespace
2. **Better Type Safety**: Proper keyword normalization
3. **Comprehensive Coverage**: All field metadata accessible
4. **Performance**: Optimized for common operations
5. **Testability**: Isolated, testable functions

## 🔗 Integration Examples

### With Type Conversion

```clojure
(require '[app.shared.field-metadata :as field-meta]
         '[app.shared.type-conversion :as type-conv])

(defn prepare-field-for-db [models-data entity-key field-name value]
  (let [field-type (field-meta/get-field-type models-data entity-key field-name)]
    (type-conv/cast-field-value field-type value)))
```

### With Validation

```clojure
(require '[app.shared.field-metadata :as field-meta]
         '[app.shared.validation.field-types :as field-types])

(defn validate-entity-field [models-data entity-key field-name value]
  (let [field-type (field-meta/get-field-type models-data entity-key field-name)
        required? (field-meta/is-required-field? models-data entity-key field-name)]
    (field-types/validate-field field-type value required?)))
```

## 🔗 Related Documentation

- [Type Conversion](type-conversion.md)
- [Validation System](validation.md)
- [Schema Definitions](schemas.md)
- [Database Integration](../backend/database.md)
- [Form Components](../frontend/component-guide.md)

---

**Note**: The field metadata system provides the foundation for dynamic form generation, validation, and database operations throughout the application. It serves as the central source of truth for all field-related information.
