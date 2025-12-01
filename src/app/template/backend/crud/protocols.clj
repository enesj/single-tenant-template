(ns app.template.backend.crud.protocols
  "Generic CRUD operation protocols for the template infrastructure.

   These protocols define the contract for metadata-driven CRUD operations
   that can be used by any domain entity. They support:
   - Metadata-driven operations based on models.edn
   - Multi-tenant data isolation
   - Automatic field validation and type casting
   - Query building with foreign key joins
   - Batch operations for performance

   All operations are context-aware and respect tenant boundaries."
  (:require
    [clojure.spec.alpha :as s]))

;; ============================================================================
;; Core CRUD Protocol
;; ============================================================================

(defprotocol CrudService
  "Core CRUD operations for entities with metadata-driven behavior."

  (get-items [this tenant-id entity-key opts]
    "Retrieve multiple items for an entity.

     Args:
       tenant-id: The tenant context (required for multi-tenancy)
       entity-key: The entity type (e.g., :properties, :users)
       opts: Optional query parameters:
         - :filters - Map of field filters
         - :order-by - Ordering specification
         - :limit - Maximum number of results
         - :offset - Query offset for pagination
         - :include-joins? - Whether to include foreign key joins

     Returns:
       Vector of entity maps with foreign key data resolved")

  (get-item [this tenant-id entity-key item-id opts]
    "Retrieve a single item by ID.

     Args:
       tenant-id: The tenant context
       entity-key: The entity type
       item-id: The unique identifier
       opts: Optional parameters:
         - :include-joins? - Whether to include foreign key joins

     Returns:
       Entity map or nil if not found")

  (create-item [this tenant-id entity-key data opts]
    "Create a new item.

     Args:
       tenant-id: The tenant context
       entity-key: The entity type
       data: Map of field values to create
       opts: Optional parameters:
         - :validate? - Whether to validate before creation (default true)
         - :return-joins? - Whether to return with foreign key data

     Returns:
       Created entity map with generated fields (id, timestamps)")

  (update-item [this tenant-id entity-key item-id data opts]
    "Update an existing item.

     Args:
       tenant-id: The tenant context
       entity-key: The entity type
       item-id: The unique identifier
       data: Map of field values to update
       opts: Optional parameters:
         - :validate? - Whether to validate before update (default true)
         - :return-joins? - Whether to return with foreign key data

     Returns:
       Updated entity map or nil if not found")

  (delete-item [this tenant-id entity-key item-id opts]
    "Delete an item by ID.

     Args:
       tenant-id: The tenant context
       entity-key: The entity type
       item-id: The unique identifier
       opts: Optional parameters (for future extension)

     Returns:
       Map with :id of deleted item or nil if not found")

  (batch-update-items [this tenant-id entity-key items opts]
    "Update multiple items in a batch operation.

     Args:
       tenant-id: The tenant context
       entity-key: The entity type
       items: Vector of maps, each containing :id and fields to update
       opts: Optional parameters:
         - :validate? - Whether to validate each item (default true)
         - :continue-on-error? - Whether to continue if one item fails

     Returns:
       Map with :updated count and :results vector")

  (batch-delete-items [this tenant-id entity-key item-ids opts]
    "Delete multiple items in a batch operation.

     Args:
       tenant-id: The tenant context
       entity-key: The entity type
       item-ids: Vector of unique identifiers to delete
       opts: Optional parameters (for future extension)

     Returns:
       Map with :deleted count, :deleted-ids, and :not-found-ids"))

;; ============================================================================
;; Metadata Service Protocol
;; ============================================================================

(defprotocol MetadataService
  "Service for working with entity metadata from models.edn."

  (get-entity-metadata [this entity-key]
    "Get complete metadata for an entity.

     Args:
       entity-key: The entity type

     Returns:
       Map with entity definition including fields, types, constraints")

  (get-field-metadata [this entity-key field-name]
    "Get metadata for a specific field.

     Args:
       entity-key: The entity type
       field-name: The field name

     Returns:
       Map with field type, constraints, and validation rules")

  (get-foreign-keys [this entity-key]
    "Get foreign key relationships for an entity.

     Args:
       entity-key: The entity type

     Returns:
       Vector of maps with :field, :foreign-table, :foreign-field")

  (validate-entity-exists [this entity-key]
    "Check if an entity exists in the metadata.

     Args:
       entity-key: The entity type

     Returns:
       Boolean indicating existence")

  (get-entity-field-specs [this entity-key opts]
    "Get field specifications for forms or tables.

     Args:
       entity-key: The entity type
       opts: Options:
         - :exclude-form-fields? - Exclude form-only fields
         - :include-readonly? - Include readonly fields

     Returns:
       Vector of field specification maps"))

;; ============================================================================
;; Validation Service Protocol
;; ============================================================================

(defprotocol ValidationService
  "Service for validating entity data against metadata rules."

  (validate-field [this entity-key field-name value]
    "Validate a single field value.

     Args:
       entity-key: The entity type
       field-name: The field name
       value: The value to validate

     Returns:
       Map with :valid? and optional :message, :code")

  (validate-entity [this entity-key data]
    "Validate all fields in an entity data map.

     Args:
       entity-key: The entity type
       data: Map of field values to validate

     Returns:
       Map with :valid? and optional :errors vector")

  (validate-required-fields [this entity-key data]
    "Check that all required fields are present.

     Args:
       entity-key: The entity type
       data: Map of field values

     Returns:
       Map with :valid? and optional :missing-fields vector")

  (validate-foreign-keys [this tenant-id entity-key data]
    "Validate that foreign key references exist.

     Args:
       tenant-id: The tenant context for multi-tenant validation
       entity-key: The entity type
       data: Map of field values

     Returns:
       Map with :valid? and optional :invalid-references vector"))

;; ============================================================================
;; Query Builder Protocol
;; ============================================================================

(defprotocol QueryBuilder
  "Service for building database queries with metadata awareness."

  (build-select-query [this entity-key opts]
    "Build a SELECT query for an entity.

     Args:
       entity-key: The entity type
       opts: Query options:
         - :filters - Map of field filters
         - :include-joins? - Whether to add foreign key joins
         - :order-by - Ordering specification
         - :limit - Maximum results
         - :offset - Query offset

     Returns:
       HoneySQL query map")

  (build-insert-query [this entity-key data]
    "Build an INSERT query for an entity.

     Args:
       entity-key: The entity type
       data: Map of field values (already cast and validated)

     Returns:
       HoneySQL query map")

  (build-update-query [this entity-key item-id data]
    "Build an UPDATE query for an entity.

     Args:
       entity-key: The entity type
       item-id: The unique identifier
       data: Map of field values (already cast and validated)

     Returns:
       HoneySQL query map")

  (build-delete-query [this entity-key item-id]
    "Build a DELETE query for an entity.

     Args:
       entity-key: The entity type
       item-id: The unique identifier

     Returns:
       HoneySQL query map"))

;; ============================================================================
;; Type Casting Protocol
;; ============================================================================

(defprotocol TypeCastingService
  "Service for casting data types according to entity metadata."

  (cast-for-insert [this entity-key data]
    "Cast data for INSERT operations.

     Args:
       entity-key: The entity type
       data: Map of field values

     Returns:
       Map with values cast to appropriate database types")

  (cast-for-update [this entity-key data]
    "Cast data for UPDATE operations.

     Args:
       entity-key: The entity type
       data: Map of field values

     Returns:
       Map with values cast and updated_at added")

  (cast-field-value [this entity-key field-name value]
    "Cast a single field value.

     Args:
       entity-key: The entity type
       field-name: The field name
       value: The value to cast

     Returns:
       Casted value ready for database"))

;; ============================================================================
;; Specs for Protocol Arguments
;; ============================================================================

(s/def ::tenant-id uuid?)
(s/def ::entity-key keyword?)
(s/def ::item-id uuid?)
(s/def ::field-name keyword?)
(s/def ::data map?)
(s/def ::opts map?)

;; CRUD operation specs
(s/def ::crud-filters map?)
(s/def ::crud-order-by (s/or :keyword keyword? :vector vector?))
(s/def ::crud-limit pos-int?)
(s/def ::crud-offset nat-int?)

(s/def ::get-items-opts
  (s/keys :opt-un [::crud-filters ::crud-order-by ::crud-limit ::crud-offset]))

(s/def ::create-item-opts
  (s/keys :opt-un [::validate? ::return-joins?]))

(s/def ::update-item-opts
  (s/keys :opt-un [::validate? ::return-joins?]))
