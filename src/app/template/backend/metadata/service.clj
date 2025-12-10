(ns app.template.backend.metadata.service
  "Metadata service implementation for handling entity models.

   This service provides metadata-driven operations based on models.edn,
   including field type resolution, entity introspection, and field
   specification generation for forms and tables."
  (:require
    [app.shared.field-metadata :as field-meta]
    [app.shared.field-specs :as field-specs]
    [app.shared.model-naming :as model-naming]
    [app.shared.type-conversion :as type-conv]
    [app.template.backend.crud.protocols :as crud-protocols]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Metadata Service Implementation
;; ============================================================================

;; ============================================================================
;; Helper Functions
;; ============================================================================

;; ============================================================================
;; Metadata Service Implementation
;; ============================================================================

;; ============================================================================
;; Helper Functions
;; ============================================================================

(defn- entity-definition*
  [models entity-key]
  (model-naming/entity-definition models entity-key))

(defn- app-entity-key
  [models entity-key]
  (some-> (entity-definition* models entity-key) :app/entity))

(defn- db-entity-key
  [models entity-key]
  (let [entity (entity-definition* models entity-key)]
    (when entity
      (or (:db/entity entity)
        (model-naming/app-entity->db models (:app/entity entity))))))

(defn- app-field-key
  [entity-def field-name]
  (let [field-kw (if (keyword? field-name) field-name (keyword field-name))]
    (if entity-def
      (model-naming/db-field->app entity-def field-kw)
      (model-naming/db-keyword->app field-kw))))

(defn- db-field-key
  [entity-def field-name]
  (let [field-kw (if (keyword? field-name) field-name (keyword field-name))]
    (if entity-def
      (model-naming/app-field->db entity-def field-kw)
      (model-naming/app-keyword->db field-kw))))

(defn- safe-sql-name
  [k]
  (-> (name k) (str/replace "-" "_")))

(defn- normalize-order-token
  [entity-def token]
  (if (keyword? token)
    (let [token-name (name token)]
      (if (str/includes? token-name ".")
        token
        (or (db-field-key entity-def token) token)))
    token))

(defn- normalize-order-entry
  [entity-def entry]
  (cond
    (keyword? entry) (normalize-order-token entity-def entry)
    (vector? entry)
    (let [[field & rest] entry]
      (if (keyword? field)
        (into [(normalize-order-token entity-def field)] rest)
        entry))
    :else entry))

(defn- normalize-order-by
  [models entity-key order-by]
  (let [entity-def (entity-definition* models entity-key)]
    (cond
      (nil? order-by) nil
      (keyword? order-by) [(normalize-order-token entity-def order-by)]
      (vector? order-by) (mapv #(normalize-order-entry entity-def %) order-by)
      (sequential? order-by) (map #(normalize-order-entry entity-def %) order-by)
      :else order-by)))

(defn- entity-foreign-keys
  [models entity-key]
  (when-let [entity (entity-definition* models entity-key)]
    (let [fields (:fields entity)]
      (->> fields
        (keep (fn [[fname _ constraints]]
                (when-let [fk (:foreign-key constraints)]
                  (let [fk-kw (if (keyword? fk) fk (keyword fk))
                        fk-app (model-naming/db-keyword->app fk-kw)
                        foreign-entity (keyword (namespace fk-kw))
                        foreign-field (keyword (name fk-app))
                        foreign-entity-def (entity-definition* models foreign-entity)]
                    {:field fname
                     :db/field (db-field-key entity fname)
                     :foreign-table (model-naming/db-keyword->app foreign-entity)
                     :db/foreign-table (db-entity-key models foreign-entity)
                     :foreign-field foreign-field
                     :db/foreign-field (db-field-key foreign-entity-def foreign-field)}))))
        vec))))

(defn- build-foreign-key-joins
  "Add foreign key joins to a query based on entity metadata."
  [query-builder entity-key base-query]
  (let [models (:models query-builder)
        foreign-keys (crud-protocols/get-foreign-keys query-builder entity-key)
        db-entity (or (db-entity-key models entity-key) entity-key)
        table-name (safe-sql-name db-entity)]
    (if (empty? foreign-keys)
      base-query
      (let [select-fields (concat
                            [:*]
                            (mapcat (fn [fk]
                                      (let [field (:field fk)
                                            db-field (:db/field fk)
                                            foreign-table (:foreign-table fk)
                                            db-foreign-table (:db/foreign-table fk)
                                            alias-name (safe-sql-name (or db-field field))
                                            table-alias (keyword (str (safe-sql-name (or db-foreign-table foreign-table)) "_join"))]
                                        [[(keyword (str (name table-alias) ".id")) (keyword (str alias-name "_id"))]
                                         [(keyword (str (name table-alias) ".name")) (keyword (str alias-name "_name"))]]))
                              foreign-keys))

            join-clauses (mapcat (fn [fk]
                                   (let [field (:field fk)
                                         db-field (:db/field fk)
                                         foreign-table (:foreign-table fk)
                                         db-foreign-table (:db/foreign-table fk)
                                         foreign-field (:foreign-field fk)
                                         db-foreign-field (:db/foreign-field fk)
                                         join-table-name (keyword (safe-sql-name (or db-foreign-table foreign-table)))
                                         table-alias (keyword (str (name join-table-name) "_join"))
                                         join-table (keyword (str table-name "." (safe-sql-name (or db-field field))))
                                         join-with (keyword (str (name table-alias) "." (safe-sql-name (or db-foreign-field foreign-field))))]
                                     [[join-table-name table-alias]
                                      [:= join-table join-with]]))
                           foreign-keys)]

        (-> base-query
          (assoc :select select-fields)
          (assoc :left-join join-clauses))))))

;; ============================================================================
;; Metadata Service Implementation
;; ============================================================================

(defrecord TemplateMetadataService [models]
  crud-protocols/MetadataService

  (get-entity-metadata [_ entity-key]
    (when-let [entity (entity-definition* models entity-key)]
      {:entity (:app/entity entity)
       :db/entity (:db/entity entity)
       :fields (:fields entity)
       :types (:types entity)
       :constraints (:constraints entity)
       :indexes (:indexes entity)
       :aliases {:db (:db/field-aliases entity)
                 :app (:app/field-aliases entity)}}))

  (get-field-metadata [_ entity-key field-name]
    (when-let [entity (entity-definition* models entity-key)]
      (let [app-field (app-field-key entity field-name)]
        (some (fn [[fname ftype constraints]]
                (when (= fname app-field)
                  {:entity (:app/entity entity)
                   :db/entity (:db/entity entity)
                   :field-name fname
                   :db/field-name (db-field-key entity fname)
                   :field-type ftype
                   :constraints constraints}))
          (:fields entity)))))

  (get-foreign-keys [_ entity-key]
    (entity-foreign-keys models entity-key))

  (validate-entity-exists [_ entity-key]
    (boolean (entity-definition* models entity-key)))

  (get-entity-field-specs [_ entity-key opts]
    (let [exclude-form-fields? (:exclude-form-fields? opts false)
          specs-fn (if exclude-form-fields?
                     field-specs/entity-specs
                     field-specs/form-entity-specs)
          app-key (app-entity-key models entity-key)]
      (when app-key
        (get (specs-fn models) app-key)))))

;; ============================================================================
;; Type Casting Service Implementation
;; ============================================================================

(defrecord TemplateTypeCastingService [models]
  crud-protocols/TypeCastingService

  (cast-for-insert [_ entity-key data]
    (let [entity (entity-definition* models entity-key)]
      (reduce-kv
        (fn [acc field-name value]
          (let [app-field (or (app-field-key entity field-name) (keyword field-name))
                field-type (field-meta/get-field-type models (or (:app/entity entity) entity-key) app-field)
                casted-value (if field-type
                               (type-conv/cast-field-value field-type value)
                               value)]
            (assoc acc app-field casted-value)))
        {}
        data)))

  (cast-for-update [_ entity-key data]
    (let [entity (entity-definition* models entity-key)
          cast-data (reduce-kv
                      (fn [acc field-name value]
                        (let [app-field (or (app-field-key entity field-name) (keyword field-name))
                              field-type (field-meta/get-field-type models (or (:app/entity entity) entity-key) app-field)
                              casted-value (if field-type
                                             (type-conv/cast-field-value field-type value)
                                             value)]
                          (assoc acc app-field casted-value)))
                      {}
                      data)]
      (-> cast-data
        (dissoc :tenant-id :owner-id :tenant_id :owner_id)
        (assoc :updated-at [:cast (java.time.LocalDateTime/now) :timestamptz]))))

  (cast-field-value [_ entity-key field-name value]
    (let [entity (entity-definition* models entity-key)
          app-field (or (app-field-key entity field-name) (keyword field-name))
          field-type (field-meta/get-field-type models (or (:app/entity entity) entity-key) app-field)]
      (if field-type
        (type-conv/cast-field-value field-type value)
        value))))

;; ============================================================================
;; Validation Service Implementation
;; ============================================================================

(defrecord TemplateValidationService [models db-service]
  crud-protocols/ValidationService

  (validate-field [_ entity-key field-name value]
    (try
      (let [entity (entity-definition* models entity-key)
            app-field (when entity (app-field-key entity field-name))
            field-metadata (when (and entity app-field)
                             (field-meta/get-field-spec models (:app/entity entity) app-field))]
        (cond
          (nil? entity)
          {:valid? false :message (str "Unknown entity: " entity-key)}

          (nil? field-metadata)
          {:valid? false :message (str "Unknown field: " field-name)}

          :else
          (let [[_ field-type constraints] field-metadata
                type-valid? (case (type-conv/get-base-type field-type)
                              :uuid (or (nil? value)
                                      (uuid? value)
                                      (re-matches #"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$" (str value)))
                              :integer (or (nil? value)
                                         (integer? value)
                                         (and (string? value) (re-matches #"^\d+$" value)))
                              :decimal (or (nil? value)
                                         (number? value)
                                         (and (string? value) (re-matches #"^\d+(\.\d+)?$" value)))
                              :varchar (or (nil? value) (string? value))
                              :text (or (nil? value) (string? value))
                              :boolean (or (nil? value) (boolean? value))
                              :jsonb true
                              :enum (or (nil? value) (string? value))
                              true)
                required? (false? (:null constraints))
                required-valid? (or (not required?)
                                  (and (some? value)
                                    (if (#{:varchar :text} (type-conv/get-base-type field-type))
                                      (not (str/blank? value))
                                      true)))
                valid? (and type-valid? required-valid?)
                message (cond
                          (not type-valid?) (str "Invalid type for " app-field)
                          (not required-valid?) (str app-field " is required")
                          :else nil)]
            {:valid? valid? :message message})))
      (catch Exception e
        (log/error e "Error validating field" field-name "for entity" entity-key)
        {:valid? false :message "Validation error"})))

  (validate-entity [this entity-key data]
    (try
      (let [validations (for [[field-name value] data
                              :when (not= field-name :id)]
                          (assoc (crud-protocols/validate-field this entity-key field-name value)
                            :field field-name))
            errors (filter #(not (:valid? %)) validations)
            valid? (empty? errors)]

        {:valid? valid? :errors (vec errors)})
      (catch Exception e
        (log/error e "Error validating entity" entity-key)
        {:valid? false :errors [{:message "Entity validation error"}]})))

  (validate-required-fields [_ entity-key data]
    (when-let [entity (entity-definition* models entity-key)]
      (let [required-fields (->> (:fields entity)
                              (filter (fn [[field-name _ constraints]]
                                        (and (false? (:null constraints))
                                          (not (:primary-key constraints))
                                          (not (#{:created-at :updated-at} field-name)))))
                              (map first)
                              set)
            provided-fields (->> (keys data)
                              (map (fn [k]
                                     (let [kw (if (keyword? k) k (keyword k))]
                                       (app-field-key entity kw))))
                              (remove nil?)
                              set)
            missing (seq (remove provided-fields required-fields))]
        {:valid? (nil? missing)
         :missing-fields (vec missing)})))

  (validate-foreign-keys [_this _tenant-id _entity-key _data]
    ;; This would require database access to validate foreign key references
    ;; For now, return valid - can be enhanced later
    {:valid? true :invalid-references []}))

;; ============================================================================
;; Query Builder Service Implementation
;; ============================================================================

(defrecord TemplateQueryBuilder [models]
  crud-protocols/QueryBuilder

  (build-select-query [this entity-key opts]
    (let [db-table (or (db-entity-key models entity-key) entity-key)
          from-table (keyword (safe-sql-name db-table))
          raw-filters (:filters opts)
          filters (when (map? raw-filters)
                    (model-naming/app-filters->db models entity-key raw-filters))
          where-clause (when (seq filters)
                         (let [preds (map (fn [[field value]]
                                            [:= field value])
                                       filters)]
                           (if (= 1 (count preds))
                             (first preds)
                             (into [:and] preds))))
          order-by (normalize-order-by models entity-key (:order-by opts))
          base-query (cond-> {:select [:*]
                              :from [from-table]}
                       where-clause (assoc :where where-clause)
                       order-by (assoc :order-by order-by)
                       (:limit opts) (assoc :limit (:limit opts))
                       (:offset opts) (assoc :offset (:offset opts)))
          final-query (if (:include-joins? opts)
                        (build-foreign-key-joins this entity-key base-query)
                        base-query)]
      final-query))

  (build-insert-query [_ entity-key data]
    (let [db-table (or (db-entity-key models entity-key) entity-key)
          db-data (model-naming/app-map->db models entity-key data)]
      {:insert-into [(keyword (safe-sql-name db-table))]
       :values [(into {} (remove (comp nil? val)) db-data)]
       :returning [:*]}))

  (build-update-query [_ entity-key item-id data]
    (let [db-table (or (db-entity-key models entity-key) entity-key)
          db-data (model-naming/app-map->db models entity-key data)]
      {:update [(keyword (safe-sql-name db-table))]
       :set (into {} (remove (comp nil? val)) db-data)
       :where [:= :id [:cast item-id :uuid]]
       :returning [:*]}))

  (build-delete-query [_ entity-key item-id]
    (let [db-table (or (db-entity-key models entity-key) entity-key)]
      {:delete-from [(keyword (safe-sql-name db-table))]
       :where [:= :id [:cast item-id :uuid]]
       :returning [:id]})))

;; ============================================================================
;; Factory Functions
;; ============================================================================

(defn create-metadata-service
  "Create a new metadata service instance."
  [models]
  (->TemplateMetadataService models))

(defn create-type-casting-service
  "Create a new type casting service instance."
  [models]
  (->TemplateTypeCastingService models))

(defn create-validation-service
  "Create a new validation service instance."
  [models db-service]
  (->TemplateValidationService models db-service))

(defn create-query-builder
  "Create a new query builder service instance."
  [models]
  (->TemplateQueryBuilder models))
