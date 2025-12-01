(ns app.template.backend.db.adapter
  "Database adapter implementation for template infrastructure"
  (:require
    [app.shared.adapters.database :refer [convert-pg-objects]]
    [app.shared.field-casting :as field-casting]
    [app.template.backend.db.protocols :as db-protocols]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as next-jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    (java.sql Timestamp)))
;; Automatically convert java.sql.Timestamp to java.time.LocalDateTime
(extend-protocol rs/ReadableColumn
  Timestamp
  (read-column-by-label [^Timestamp v _]
    (.toLocalDateTime v))
  (read-column-by-index [^Timestamp v _2 _3]
    (.toLocalDateTime v)))

(defn log-sql-debug
  "Log detailed SQL debug information for troubleshooting"
  [sql-map _operation]
  (try
    (let [formatted (sql/format sql-map)]
      ;;(log/info "\n=== SQL DEBUG ===" _operation "===")
      ;;(log/info "SQL Map:" sql-map)
      ;;(log/info "Generated SQL:" (first formatted))
      ;;(log/info "Parameters:" (rest formatted))
      ;;(log/info "Parameter count:" (count (rest formatted)))
      ;;(log/info "=== END SQL DEBUG ===\n")
      formatted)
    (catch Exception e
      (log/error "ERROR formatting SQL:" sql-map)
      (log/error "Exception:" (.getMessage e))
      (throw e))))

(defn create-foreign-key-error-message
  "Create a detailed error message explaining foreign key constraint violations.

   Uses a configurable relationship mapping that can be injected by the domain layer.
   Falls back to a generic message if no specific mapping is provided."
  [table _error-msg relationship-mappings]
  (let [table-keyword (keyword table)
        relationship-info (get relationship-mappings table-keyword)
        dependencies (:dependencies relationship-info)
        description (:description relationship-info)]

    (if relationship-info
      (str description " " (str/join ", " dependencies) ". "
        "Please remove or reassign these dependent records before deleting this " (name table-keyword) ".")
      "This record cannot be deleted because it is referenced by other data. Please remove dependent records first.")))

(defrecord PostgresAdapter [connection relationship-mappings]
  db-protocols/DatabaseAdapter
  (find-by-id [_this table id]
    (try
      (let [;; Cast UUID strings to proper UUID type for PostgreSQL
            uuid-id (if (string? id) [:cast id :uuid] id)
            result (next-jdbc/execute-one! connection
                     (sql/format {:select [:*]
                                  :from   [table]
                                  :where  [:= :id uuid-id]}))]
        (convert-pg-objects result))
      (catch Exception e
        (log/error "Error finding record by ID:" (.getMessage e))
        nil)))

  (find-by-field [_this table field value]
    (try
      (let [result (next-jdbc/execute-one! connection
                     (sql/format {:select [:*]
                                  :from   [table]
                                  :where  [:= field value]}))]
        (convert-pg-objects result))
      (catch Exception e
        (log/error "Error finding record by field:" (.getMessage e))
        nil)))

  (find-all [_this table]
    (try
      (let [results (next-jdbc/execute! connection
                      (sql/format {:select [:*]
                                   :from   [table]}))]
        (mapv convert-pg-objects results))
      (catch Exception e
        (log/error "Error finding all records:" (.getMessage e))
        [])))

  (list-with-filters [_this table filters]
    (try
      ;;(log/debug "🔍 DB DEBUG: list-with-filters called for table:" table "filters:" filters)
      (let [start-time (System/currentTimeMillis)
            where-clauses (for [[field value] filters]
                            [:= field value])
            ;;_ (log/debug "🔍 DB DEBUG: Generated where-clauses:" where-clauses)
            where-clause  (cond
                            (empty? where-clauses) nil
                            (> (count where-clauses) 1) (into [:and] where-clauses)
                            :else (first where-clauses))
            ;;_ (log/debug "🔍 DB DEBUG: Final where-clause:" where-clause)
            query         (cond-> {:select [:*]
                                   :from   [table]}
                            where-clause (assoc :where where-clause))
            ;;_ (log/debug "🔍 DB DEBUG: Constructed query:" query)
            ;;_ (log/debug "🔍 DB DEBUG: About to format SQL...")
            formatted-sql (sql/format query)
            ;;_ (log/debug "🔍 DB DEBUG: Formatted SQL:" formatted-sql)
            ;;_ (log/debug "🔍 DB DEBUG: About to execute SQL...")
            results (next-jdbc/execute! connection formatted-sql)
            end-time (System/currentTimeMillis)
            _duration (- end-time start-time)]
        ;;(log/debug "🔍 DB DEBUG: SQL execution completed in" _duration "ms")
        ;;(log/debug "🔍 DB DEBUG: Result count:" (count results))
        ;;(log/debug "🔍 DB DEBUG: About to convert PostgreSQL objects in list-with-filters...")
        (mapv convert-pg-objects results))
      (catch Exception e
        (log/error "🔍 DB DEBUG: Exception in list-with-filters:" (.getMessage e))
        (log/error "🔍 DB DEBUG: Exception type:" (type e))
        (log/error "🔍 DB DEBUG: Exception stack:" e)
        (log/error "Error listing records with filters:" (.getMessage e))
        [])))

  (create [_this metadata table data]
    (try
      (let [processed-data (field-casting/prepare-insert-data metadata table data)
            insert-map     {:insert-into [table]
                            :values      [processed-data]
                            :returning   [:*]}
            formatted-sql  (log-sql-debug insert-map (str "INSERT INTO " table))]
        ;;(log/info "Processed data for table:" table "data:" processed-data)
        (if (empty? processed-data)
          (do
            (log/error "ABORTING: Empty processed data for table:" table "original data:" data)
            (throw (ex-info "Empty insert data" {:table     table
                                                 :data      data
                                                 :processed processed-data})))
          (let [result (next-jdbc/execute-one! connection formatted-sql)
                converted-result (convert-pg-objects result)]
            ;;(log/info "Database insert result:" result)
            ;;(log/info "Converted result:" converted-result)
            converted-result)))
      (catch Exception e
        (log/error "Database error for table:" table)
        (log/error "Original data:" data)
        (log/error "Error:" (.getMessage e))
        (throw e))))

  (update-record [_this metadata table id data]
    (try
      (let [processed-data (field-casting/prepare-update-data metadata table data)
            ;; Cast UUID strings to proper UUID type for PostgreSQL
            uuid-id (if (string? id) [:cast id :uuid] id)
            result (next-jdbc/execute-one! connection
                     (sql/format {:update    [table]
                                  :set       processed-data
                                  :where     [:= :id uuid-id]
                                  :returning [:*]}))]
        (convert-pg-objects result))
      (catch Exception e
        (log/error "Error updating record:" (.getMessage e))
        (throw e))))

  (delete [_this table id]
    (try
      (let [;; Cast UUID strings to proper UUID type for PostgreSQL
            uuid-id (if (string? id) [:cast id :uuid] id)]
        (next-jdbc/execute-one! connection
          (sql/format {:delete-from [table]
                       :where       [:= :id uuid-id]}))
        {:success? true})
      (catch Exception e
        (let [error-msg (.getMessage e)]
          (log/error "Error deleting record:" error-msg)
          (cond
            ;; Check for foreign key constraint violations
            (re-find #"violates foreign key constraint" error-msg)
            (let [detailed-message (create-foreign-key-error-message table error-msg (or relationship-mappings {}))]
              (throw (ex-info "Cannot delete: record is referenced by other data"
                       {:type :foreign-key-constraint
                        :table table
                        :id id
                        :message detailed-message})))

            ;; Other database errors
            :else
            (throw (ex-info "Database error during deletion"
                     {:type :database-error
                      :table table
                      :id id
                      :message error-msg})))))))

  (delete [_this table id opts]
    (try
      (let [;; Cast UUID strings to proper UUID type for PostgreSQL
            uuid-id (if (string? id) [:cast id :uuid] id)
            is-admin? (:is-admin? opts)]
        (if is-admin?
          ;; Admin context - use transaction with RLS bypass
          (let [_result (next-jdbc/with-transaction [tx connection]
                          ;; Set admin bypass context
                          (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
                          ;; Execute the delete
                          (next-jdbc/execute-one! tx
                            (sql/format {:delete-from [table]
                                         :where       [:= :id uuid-id]})))]
            {:success? true})
          ;; Regular context - normal delete
          (do
            (next-jdbc/execute-one! connection
              (sql/format {:delete-from [table]
                           :where       [:= :id uuid-id]}))
            {:success? true})))
      (catch Exception e
        (let [error-msg (.getMessage e)]
          (log/error "Error deleting record:" error-msg)
          (cond
            ;; Check for foreign key constraint violations
            (re-find #"violates foreign key constraint" error-msg)
            (let [detailed-message (create-foreign-key-error-message table error-msg (or relationship-mappings {}))]
              (throw (ex-info "Cannot delete: record is referenced by other data"
                       {:type :foreign-key-constraint
                        :table table
                        :id id
                        :message detailed-message})))

            ;; Other database errors
            :else
            (throw (ex-info "Database error during deletion"
                     {:type :database-error
                      :table table
                      :id id
                      :message error-msg})))))))

  (exists? [_this table field value]
    (try
      (let [result (next-jdbc/execute-one! connection
                     (sql/format {:select [1]
                                  :from   [table]
                                  :where  [:= field value]
                                  :limit  1}))]
        (not (nil? result)))
      (catch Exception e
        (log/error "Error checking if record exists:" (.getMessage e))
        false)))

  (execute! [_this sql params]
    (try
      (let [result (next-jdbc/execute! connection (into [sql] params))]
        (mapv convert-pg-objects result))
      (catch Exception e
        (log/error "Error executing raw SQL:" sql "params:" params (.getMessage e))
        (throw e))))

  db-protocols/TransactionManager
  (with-transaction [_this f]
    ;; For now, delegate to the existing transaction mechanism
    ;; In a more sophisticated implementation, this would handle nested transactions
    ;; Use next.jdbc transaction support
    (next-jdbc/with-transaction [tx connection]
      (f (->PostgresAdapter tx relationship-mappings))))

  (rollback [_this]
    ;; Implementation would depend on transaction state management
    (throw (ex-info "Manual rollback not implemented" {})))

  (commit [_this]
    ;; Implementation would depend on transaction state management
    (throw (ex-info "Manual commit not implemented" {}))))

(defn create-postgres-adapter
  "Create PostgreSQL database adapter with optional relationship mappings"
  ([connection]
   (->PostgresAdapter connection nil))
  ([connection relationship-mappings]
   (->PostgresAdapter connection relationship-mappings)))
