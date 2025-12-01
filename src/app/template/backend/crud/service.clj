(ns app.template.backend.crud.service
  "Main CRUD service implementation for the template infrastructure.

   This service provides complete metadata-driven CRUD operations by
   orchestrating the metadata, validation, type casting, and query
   building services."
  (:require
    [app.shared.model-naming :as model-naming]
    [app.template.backend.crud.protocols :as crud-protocols]
    [app.template.backend.db.protocols :as db-protocols]
    [taoensso.timbre :as log]))

;; ============================================================================
;; CRUD Service Implementation
;; ============================================================================

(defn- entity-has-tenant-id?
  "Check if an entity has a tenant-id field"
  [metadata-service entity-key]
  (some? (crud-protocols/get-field-metadata metadata-service entity-key :tenant-id)))

(defn- ->keyword
  [v]
  (cond
    (keyword? v) v
    (string? v) (keyword v)
    (nil? v) nil
    :else (keyword (str v))))

(defn- app-entity-key
  [entity-metadata entity-key]
  (let [entity-kw (->keyword entity-key)
        app-key (:entity entity-metadata)]
    (or app-key (model-naming/db-keyword->app entity-kw))))

(defn- db-entity-key
  [entity-metadata entity-key]
  (let [app-key (app-entity-key entity-metadata entity-key)
        db-key (:db/entity entity-metadata)]
    (or db-key (model-naming/app-keyword->db app-key))))

(defn- alias->app
  [entity-metadata field]
  (if (keyword? field)
    (let [aliases (or (get-in entity-metadata [:aliases :db]) {})]
      (or (get aliases field)
        (model-naming/db-keyword->app field)))
    field))

(defn- alias->db
  [entity-metadata field]
  (if (keyword? field)
    (let [aliases (or (get-in entity-metadata [:aliases :app]) {})]
      (or (get aliases field)
        (model-naming/app-keyword->db field)))
    field))

(defn- convert-map
  [entity-metadata data direction]
  (when (map? data)
    (reduce-kv
      (fn [acc k v]
        (assoc acc
          (if (keyword? k)
            (case direction
              :to-app (alias->app entity-metadata k)
              :to-db (alias->db entity-metadata k)
              k)
            k)
          v))
      (empty data)
      data)))

(defn- convert-rows
  [entity-metadata rows]
  (cond
    (vector? rows) (mapv #(convert-map entity-metadata % :to-app) rows)
    (map? rows) (convert-map entity-metadata rows :to-app)
    (seq? rows) (map #(convert-map entity-metadata % :to-app) rows)
    :else rows))

(defn- entity-context
  [metadata-service entity-key]
  (let [metadata (crud-protocols/get-entity-metadata metadata-service entity-key)]
    {:metadata metadata
     :app-entity (app-entity-key metadata entity-key)
     :db-entity (db-entity-key metadata entity-key)}))

(defrecord TemplateCrudService [db-service metadata-service validation-service
                                type-casting-service query-builder]
  crud-protocols/CrudService

  (get-items [_this tenant-id entity-key opts]
    (try
      (when-not (crud-protocols/validate-entity-exists metadata-service entity-key)
        (throw (ex-info "Entity not found"
                 {:type :entity-not-found :entity entity-key})))

      (let [{:keys [metadata app-entity db-entity]} (entity-context metadata-service entity-key)
            has-tenant-id? (entity-has-tenant-id? metadata-service entity-key)
            base-filters (:filters opts {})
            filters-app (if has-tenant-id?
                          (assoc base-filters :tenant-id tenant-id)
                          base-filters)
            db-filters (convert-map metadata filters-app :to-db)
            special-tenant-fetch? (and (= app-entity :tenants)
                                    (contains? filters-app :id))
            start-time (System/currentTimeMillis)
            raw-results (if special-tenant-fetch?
                          (let [item (crud-protocols/get-item _this tenant-id entity-key (:id filters-app) {})]
                            (if item [item] []))
                          (db-protocols/list-with-filters db-service db-entity db-filters))
            end-time (System/currentTimeMillis)
            _duration (- end-time start-time)]
        (when (not special-tenant-fetch?)
          (log/trace "CRUD get-items returned" (count raw-results) "rows for" app-entity "in" _duration "ms"))
        (if special-tenant-fetch?
          raw-results
          (convert-rows metadata raw-results)))

      (catch Exception e
        (if (= :entity-not-found (:type (ex-data e)))
          (throw e)
          (do
            (log/error e "Error retrieving items for entity" entity-key)
            (throw (ex-info "CRUD operation failed"
                     {:type :crud-error :operation :get-items})))))))

  (get-item [_this tenant-id entity-key item-id _opts]
    (try
      (when-not (crud-protocols/validate-entity-exists metadata-service entity-key)
        (throw (ex-info "Entity not found"
                 {:type :entity-not-found :entity entity-key})))

      (let [{:keys [metadata db-entity]} (entity-context metadata-service entity-key)
            has-tenant-id? (entity-has-tenant-id? metadata-service entity-key)
            raw-item (db-protocols/find-by-id db-service db-entity item-id)
            item (convert-map metadata raw-item :to-app)]
        (if (and item
              (or (not has-tenant-id?)
                (= (str (:tenant-id item)) (str tenant-id))))
          item
          nil))

      (catch Exception e
        (log/error e "Error retrieving item" item-id)
        (throw (ex-info "CRUD operation failed"
                 {:type :crud-error :operation :get-item})))))

  (create-item [this tenant-id entity-key data opts]
    (try
      (when-not (crud-protocols/validate-entity-exists metadata-service entity-key)
        (throw (ex-info "Entity not found"
                 {:type :entity-not-found :entity entity-key})))

      (let [{:keys [metadata app-entity db-entity]} (entity-context metadata-service entity-key)
            has-tenant-id? (entity-has-tenant-id? metadata-service entity-key)
            data-with-tenant (if has-tenant-id?
                               (assoc data :tenant-id tenant-id)
                               data)
            user-id (:user-id opts)
            default-user-id (when (and (not user-id)
                                    (or (crud-protocols/get-field-metadata metadata-service entity-key :created-by)
                                      (crud-protocols/get-field-metadata metadata-service entity-key :updated-by)))
                              (try
                                (let [{user-meta :metadata user-db-entity :db-entity} (entity-context metadata-service :users)
                                      users (->> (db-protocols/find-all db-service user-db-entity)
                                              (convert-rows user-meta))]
                                  (when (seq users)
                                    (:id (first users))))
                                (catch Exception e
                                  (log/warn e "Could not find default user")
                                  "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")))
            actual-user-id (or user-id default-user-id)
            data-with-audit (cond-> data-with-tenant
                              (and actual-user-id (crud-protocols/get-field-metadata metadata-service entity-key :created-by))
                              (assoc :created-by actual-user-id)
                              (and actual-user-id (crud-protocols/get-field-metadata metadata-service entity-key :updated-by))
                              (assoc :updated-by actual-user-id))
            validation-result (if (:validate? opts true)
                                (crud-protocols/validate-entity validation-service entity-key data-with-audit)
                                {:valid? true})
            _ (when-not (:valid? validation-result)
                (throw (ex-info "Validation failed"
                         {:type :validation-error
                          :errors (:errors validation-result)})))
            cast-data (crud-protocols/cast-for-insert type-casting-service entity-key data-with-audit)
            clean-data (->> cast-data (filter (fn [[_k v]] (some? v))) (into {}))
            db-data (convert-map metadata clean-data :to-db)
            raw-result (db-protocols/create db-service metadata db-entity db-data)
            result (convert-map metadata raw-result :to-app)]

        (log/info "Created item for entity" app-entity "id" (:id result))

        (if (:return-joins? opts)
          (crud-protocols/get-item this tenant-id entity-key (:id result) {:include-joins? true})
          result))

      (catch Exception e
        (if (= :validation-error (:type (ex-data e)))
          (throw e)
          (do
            (log/error e "Error creating item for entity" entity-key)
            (throw (ex-info "CRUD operation failed"
                     {:type :crud-error :operation :create-item})))))))

  (update-item [this tenant-id entity-key item-id data opts]
    (try
      (when-not (crud-protocols/validate-entity-exists metadata-service entity-key)
        (throw (ex-info "Entity not found"
                 {:type :entity-not-found :entity entity-key})))

      (let [{:keys [metadata app-entity db-entity]} (entity-context metadata-service entity-key)
            existing-item (crud-protocols/get-item this tenant-id entity-key item-id {})
            _ (when-not existing-item
                (throw (ex-info "Item not found"
                         {:type :item-not-found
                          :entity entity-key
                          :item-id item-id})))
            user-id (:user-id opts)
            default-user-id (when (and (not user-id)
                                    (crud-protocols/get-field-metadata metadata-service entity-key :updated-by))
                              (try
                                (let [{user-meta :metadata user-db-entity :db-entity} (entity-context metadata-service :users)
                                      users (->> (db-protocols/find-all db-service user-db-entity)
                                              (convert-rows user-meta))]
                                  (some :id users))
                                (catch Exception _
                                  "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")))
            actual-user-id (or user-id default-user-id)
            data-with-audit (cond-> data
                              (and actual-user-id (crud-protocols/get-field-metadata metadata-service entity-key :updated-by))
                              (assoc :updated-by actual-user-id))
            validation-result (if (:validate? opts true)
                                (crud-protocols/validate-entity validation-service entity-key data-with-audit)
                                {:valid? true})
            _ (when-not (:valid? validation-result)
                (throw (ex-info "Validation failed"
                         {:type :validation-error
                          :errors (:errors validation-result)})))
            cast-data (crud-protocols/cast-for-update type-casting-service entity-key data-with-audit)
            clean-data (->> cast-data (filter (fn [[_k v]] (some? v))) (into {}))
            db-data (convert-map metadata clean-data :to-db)
            raw-result (db-protocols/update-record db-service metadata db-entity item-id db-data)
            result (convert-map metadata raw-result :to-app)]

        (log/info "Updated item" item-id "for entity" app-entity)

        (if (:return-joins? opts)
          (crud-protocols/get-item this tenant-id entity-key item-id {:include-joins? true})
          result))

      (catch Exception e
        (if (#{:validation-error :item-not-found} (:type (ex-data e)))
          (throw e)
          (do
            (log/error e "Error updating item" item-id)
            (throw (ex-info "CRUD operation failed"
                     {:type :crud-error :operation :update-item})))))))

  (delete-item [this tenant-id entity-key item-id opts]
    (try
      (when-not (crud-protocols/validate-entity-exists metadata-service entity-key)
        (throw (ex-info "Entity not found"
                 {:type :entity-not-found :entity entity-key})))

      (let [{:keys [app-entity db-entity]} (entity-context metadata-service entity-key)
            existing-item (crud-protocols/get-item this tenant-id entity-key item-id {})
            _ (when-not existing-item
                (throw (ex-info "Item not found"
                         {:type :item-not-found
                          :entity entity-key
                          :item-id item-id})))
            result (db-protocols/delete db-service db-entity item-id opts)]

        (log/info "Deleted item" item-id "for entity" app-entity)
        result)

      (catch Exception e
        (let [error-type (:type (ex-data e))]
          (case error-type
            :item-not-found (throw e)
            :foreign-key-constraint (throw e)
            :database-error (throw e)
            (do
              (log/error e "Error deleting item" item-id)
              (throw (ex-info "CRUD operation failed"
                       {:type :crud-error :operation :delete-item}))))))))

  (batch-update-items [this tenant-id entity-key items opts]
    (try
      (when-not (crud-protocols/validate-entity-exists metadata-service entity-key)
        (throw (ex-info "Entity not found"
                 {:type :entity-not-found :entity entity-key})))

      (when (empty? items)
        (throw (ex-info "No items provided for batch update"
                 {:type :validation-error :message "No items provided"})))

      (let [results (atom [])
            errors (atom [])
            updated-count (atom 0)]

        (doseq [item items]
          (try
            (let [item-id (:id item)
                  raw-data (dissoc item :id)
                  data (into {} (filter (fn [[_k v]] (and (some? v) (not= v ""))) raw-data))
                  result (crud-protocols/update-item this tenant-id entity-key item-id data
                           (assoc opts :validate? (:validate? opts true)))]
              (swap! results conj result)
              (swap! updated-count inc))
            (catch Exception e
              (log/error e "Error updating item in batch:" (:id item) "- Exception data:" (ex-data e))
              (let [error-info {:id (:id item)
                                :error (.getMessage e)
                                :type (:type (ex-data e))}]
                (swap! errors conj error-info)
                (when-not (:continue-on-error? opts)
                  (throw e))))))

        (log/info "Batch updated" @updated-count "items for entity" entity-key)

        {:updated @updated-count
         :results @results
         :errors @errors})

      (catch Exception e
        (log/error e "Error in batch update for entity" entity-key)
        (throw (ex-info "CRUD operation failed"
                 {:type :crud-error :operation :batch-update-items})))))

  (batch-delete-items [this tenant-id entity-key item-ids opts]
    (try
      (when-not (crud-protocols/validate-entity-exists metadata-service entity-key)
        (throw (ex-info "Entity not found"
                 {:type :entity-not-found :entity entity-key})))

      (when (empty? item-ids)
        (throw (ex-info "No item IDs provided for batch delete"
                 {:type :validation-error :message "No item IDs provided"})))

      (let [{:keys [db-entity]} (entity-context metadata-service entity-key)
            results (atom [])
            errors (atom [])
            deleted-count (atom 0)]

        (doseq [item-id item-ids]
          (try
            (let [existing-item (crud-protocols/get-item this tenant-id entity-key item-id {})
                  result (if existing-item
                           (do
                             (db-protocols/delete db-service db-entity item-id opts)
                             (swap! results conj item-id)
                             (swap! deleted-count inc)
                             {:deleted true :id item-id})
                           {:deleted false :id item-id :reason "not found"})]
              result)
            (catch Exception e
              (let [error-info {:id item-id
                                :error (.getMessage e)
                                :deleted false}]
                (swap! errors conj error-info)
                (when-not (:continue-on-error? opts)
                  (throw e))))))

        (log/info "Batch deleted" @deleted-count "items for entity" entity-key)

        {:deleted @deleted-count
         :deleted-ids @results
         :not-found-ids (remove (set @results) item-ids)
         :errors @errors})

      (catch Exception e
        (log/error e "Error in batch delete for entity" entity-key)
        (throw (ex-info "CRUD operation failed"
                 {:type :crud-error :operation :batch-delete-items}))))))

;; ============================================================================
;; Factory Function
;; ============================================================================

(defn create-crud-service
  "Create a new CRUD service instance with all required dependencies."
  [db-service metadata-service validation-service type-casting-service query-builder]
  (->TemplateCrudService db-service metadata-service validation-service
    type-casting-service query-builder))
