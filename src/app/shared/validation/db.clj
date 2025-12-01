(ns app.shared.validation.db
  "Database-specific validation utilities for Clojure backend."
  (:require
    [app.shared.field-metadata :as field-meta]
    [honey.sql :as sql]
    [malli.core :as m]
    [malli.error :as me]
    [next.jdbc :as jdbc]))

;; ========================================
;; Database connection validation
;; ========================================

(defn validate-connection
  "Validate database connection is working."
  [db]
  (try
    (jdbc/execute! db ["SELECT 1"])
    {:valid? true}
    (catch Exception e
      {:valid? false :error (str "Database connection failed: " (.getMessage e))})))

(defn validate-scope-context
  "Validate scoped database context is properly set (e.g., current tenant in a multi-tenant deployment)."
  [db]
  (try
    (let [result (jdbc/execute-one! db ["SELECT current_tenant_id() AS scope_id"])
          scope-id (:scope_id result)]
      (if (some? scope-id)
        {:valid? true :scope-id scope-id}
        {:valid? false :error "No scoped context set"}))
    (catch Exception e
      {:valid? false :error (str "Scoped context validation failed: " (.getMessage e))})))

;; ========================================
;; Entity existence validation
;; ========================================

(defn validate-entity-exists
  "Validate that an entity exists in the database."
  [db table-name id-field id-value]
  (try
    (let [query (sql/format {:select [:count :*]
                             :from [table-name]
                             :where [:= id-field id-value]})
          result (jdbc/execute-one! db query)
          count (:count result 0)]
      {:valid? (> count 0) :count count})
    (catch Exception e
      {:valid? false :error (str "Entity existence validation failed: " (.getMessage e))})))

(defn validate-scoped-entity-exists
  "Validate entity exists within current scoped context (e.g., tenant)."
  [db table-name id-field id-value]
  (try
    (let [query (sql/format {:select [:count :*]
                             :from [table-name]
                             :where [:and
                                     [:= id-field id-value]
                                     [:= :tenant_id [:current_tenant_id]]]})
          result (jdbc/execute-one! db query)
          count (:count result 0)]
      {:valid? (> count 0) :count count})
    (catch Exception e
      {:valid? false :error (str "Scoped entity validation failed: " (.getMessage e))})))

;; ========================================
;; Foreign key validation
;; ========================================

(defn validate-foreign-key
  "Validate foreign key reference exists."
  [db ref-table ref-field ref-value]
  (validate-entity-exists db ref-table ref-field ref-value))

(defn validate-scoped-foreign-key
  "Validate foreign key reference exists within current scope (e.g., tenant)."
  [db ref-table ref-field ref-value]
  (validate-scoped-entity-exists db ref-table ref-field ref-value))

;; ========================================
;; Uniqueness validation
;; ========================================

(defn validate-unique-value
  "Validate value is unique in table/field."
  [db table-name field-name value & [exclude-id]]
  (try
    (let [base-query {:select [:count :*]
                      :from [table-name]
                      :where [:= field-name value]}
          query (if exclude-id
                  (update base-query :where conj [:not= :id exclude-id])
                  base-query)
          result (jdbc/execute-one! db (sql/format query))
          count (:count result 0)]
      {:valid? (= count 0) :count count})
    (catch Exception e
      {:valid? false :error (str "Uniqueness validation failed: " (.getMessage e))})))

(defn validate-scoped-unique-value
  "Validate value is unique within current scope (e.g., tenant)."
  [db table-name field-name value & [exclude-id]]
  (try
    (let [base-where [:and
                      [:= field-name value]
                      [:= :tenant_id [:current_tenant_id]]]
          where-clause (if exclude-id
                         (conj base-where [:not= :id exclude-id])
                         base-where)
          query (sql/format {:select [:count :*]
                             :from [table-name]
                             :where where-clause})
          result (jdbc/execute-one! db query)
          count (:count result 0)]
      {:valid? (= count 0) :count count})
    (catch Exception e
      {:valid? false :error (str "Scoped uniqueness validation failed: " (.getMessage e))})))

;; ========================================
;; Schema validation
;; ========================================

(defn validate-table-exists
  "Validate that a table exists in the database."
  [db table-name]
  (try
    (let [query (sql/format {:select [:count :*]
                             :from [:information_schema.tables]
                             :where [:and
                                     [:= :table_name (name table-name)]
                                     [:= :table_schema "public"]]})
          result (jdbc/execute-one! db query)
          count (:count result 0)]
      {:valid? (> count 0) :count count})
    (catch Exception e
      {:valid? false :error (str "Table existence validation failed: " (.getMessage e))})))

(defn validate-column-exists
  "Validate that a column exists in a table."
  [db table-name column-name]
  (try
    (let [query (sql/format {:select [:count :*]
                             :from [:information_schema.columns]
                             :where [:and
                                     [:= :table_name (name table-name)]
                                     [:= :column_name (name column-name)]
                                     [:= :table_schema "public"]]})
          result (jdbc/execute-one! db query)
          count (:count result 0)]
      {:valid? (> count 0) :count count})
    (catch Exception e
      {:valid? false :error (str "Column existence validation failed: " (.getMessage e))})))

;; ========================================
;; Data integrity validation
;; ========================================

(defn validate-data-integrity
  "Validate data integrity for an entity."
  [db models entity-name data]
  (try
    (let [entity-key (field-meta/normalize-entity-key entity-name)
          entity-fields (get-in models [entity-key :fields])
          validation-results (atom [])]

      ;; Validate each field
      (doseq [[field-name field-type] entity-fields]
        (let [field-value (get data field-name)]
          (when (and field-value (not= field-value ""))
            (case field-type
              :reference
              (let [ref-result (validate-scoped-foreign-key db
                                 (keyword (str (name field-name) "s"))
                                 :id
                                 field-value)]
                (when-not (:valid? ref-result)
                  (swap! validation-results conj
                    {:field field-name
                     :error (str "Invalid reference: " (:error ref-result))})))

              :unique
              (let [unique-result (validate-scoped-unique-value db
                                    (keyword (str (name entity-name) "s"))
                                    field-name
                                    field-value
                                    (:id data))]
                (when-not (:valid? unique-result)
                  (swap! validation-results conj
                    {:field field-name
                     :error "Value must be unique"})))

              ;; Default: no additional validation
              nil))))

      {:valid? (empty? @validation-results)
       :errors @validation-results})
    (catch Exception e
      {:valid? false :error (str "Data integrity validation failed: " (.getMessage e))})))

;; ========================================
;; Batch validation
;; ========================================

(defn validate-batch-data
  "Validate multiple entities in a batch."
  [db models entity-name data-list]
  (try
    (let [results (mapv #(validate-data-integrity db models entity-name %) data-list)
          valid-count (count (filter :valid? results))
          total-count (count results)]
      {:valid? (= valid-count total-count)
       :valid-count valid-count
       :total-count total-count
       :results results})
    (catch Exception e
      {:valid? false :error (str "Batch validation failed: " (.getMessage e))})))

;; ========================================
;; Transaction validation
;; ========================================

(defn validate-in-transaction
  "Validate data within a database transaction."
  [db validation-fn]
  (try
    (jdbc/with-transaction [tx db]
      (validation-fn tx))
    (catch Exception e
      {:valid? false :error (str "Transaction validation failed: " (.getMessage e))})))

;; ========================================
;; Malli integration with database
;; ========================================

(defn create-database-schema
  "Create database-aware Malli schema."
  [db models entity-name]
  (let [entity-key (field-meta/normalize-entity-key entity-name)
        entity-fields (get-in models [entity-key :fields])
        schema-fields (mapv (fn [[field-name field-type]]
                              (case field-type
                                :reference
                                [field-name {:optional true}
                                 [:fn #(validate-scoped-foreign-key db
                                         (keyword (str (name field-name) "s"))
                                         :id
                                         %)]]
                                :unique
                                [field-name {:optional true}
                                 [:fn #(validate-scoped-unique-value db
                                         (keyword (str (name entity-name) "s"))
                                         field-name
                                         %)]]
                               ;; Default field
                                [field-name {:optional true} :any]))
                        entity-fields)]
    (into [:map] schema-fields)))

(defn validate-with-database-schema
  "Validate data using database-aware schema."
  [db models entity-name data]
  (try
    (let [schema (create-database-schema db models entity-name)]
      (if (m/validate schema data)
        {:valid? true :data data}
        {:valid? false :errors (me/humanize (m/explain schema data))}))
    (catch Exception e
      {:valid? false :error (str "Database schema validation failed: " (.getMessage e))})))
