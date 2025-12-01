(ns app.shared.field-casting
  "Legacy compatibility layer for field casting.

   This namespace provides backward compatibility while delegating to the new
   consolidated type-conversion and field-metadata namespaces."
  (:require
    #?(:clj [taoensso.timbre :as log]
       :cljs [taoensso.timbre :as log])
    [app.shared.field-metadata :as field-meta]
    [app.shared.type-conversion :as type-conv]))

;; ============================================================================
;; Legacy Compatibility Functions
;; ============================================================================

;; Function removed - use app.shared.field-metadata/get-field-type directly

;; Function removed - use app.shared.type-conversion/cast-for-database directly

;; ============================================================================
;; Batch Processing Functions
;; ============================================================================

(defn cast-entity-data
  "Cast all fields in an entity data map using model definitions.

   Args:
     models: The models data (passed as parameter)
     entity: The entity name (e.g., :properties, :users)
     data: Map of field-name -> value pairs

   Returns:
     Map with all values cast to appropriate PostgreSQL types"
  [models entity data]
  (type-conv/prepare-data-for-db models entity data {:include-nils? true}))

(defn prepare-insert-data
  "Prepare data for INSERT operations with proper field casting.

   Filters out nil values and casts all fields according to model definitions.

   Args:
     models: The models data
     entity: The entity name
     data: The data to insert

   Returns:
     Processed data ready for HoneySQL INSERT"
  [models entity data]
  (let [cast-data (cast-entity-data models entity data)
        filtered-data (->> cast-data
                        (filter (fn [[_k v]]
                                  (let [keep? (some? v)]
                                    keep?)))
                        (into {}))]

    (when (empty? filtered-data)
      (log/error "WARNING: Final data is empty! This will cause INSERT syntax error"))

    filtered-data))

(defn prepare-update-data
  "Prepare data for UPDATE operations with proper field casting.

   Similar to prepare-insert-data but includes updated_at timestamp
   and excludes immutable fields (tenant, owner).

   Args:
     models: The models data
     entity: The entity name
     data: The data to update

   Returns:
     Processed data ready for HoneySQL UPDATE"
  [models entity data]
  (-> (cast-entity-data models entity data)
    ;; Exclude immutable fields that should never be updated
    (dissoc :tenant :owner)
    ;; Add updated_at timestamp
    (assoc :updated_at [:cast #?(:clj (java.time.LocalDateTime/now)
                                 :cljs (js/Date.)) :timestamptz])
    ;; Remove nil values to avoid SQL issues
    (->> (filter (fn [[_k v]] (some? v)))
      (into {}))))

;; ============================================================================
;; Validation and Debugging
;; ============================================================================

(defn validate-field-casting
  "Validate that field casting is working correctly for debugging.

   Args:
     models: The models data
     entity: The entity name
     field: The field name
     value: The value to test

   Returns:
     Map with casting information for debugging"
  [models entity field value]
  (let [field-type (field-meta/get-field-type models entity field)
        casted-value (type-conv/cast-field-value field-type value)]
    {:entity entity
     :field field
     :field-type field-type
     :original-value value
     :casted-value casted-value
     :cast-applied? (not= value casted-value)}))
