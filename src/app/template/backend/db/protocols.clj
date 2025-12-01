(ns app.template.backend.db.protocols
  "Database access protocols for template infrastructure")

(defprotocol DatabaseAdapter
  "Generic database operations interface"
  (find-by-id [this table id]
    "Find record by primary key
    Returns: record map or nil")
  (find-by-field [this table field value]
    "Find record by field value
    Returns: record map or nil")
  (find-all [this table]
    "Find all records in table
    Returns: [record-map]")
  (list-with-filters [this table filters]
    "Find records matching filters
    Returns: [record-map]")
  (create [this metadata table data]
    "Create new record with validation
    Returns: created record map")
  (update-record [this metadata table id data]
    "Update existing record with validation
    Returns: updated record map")
  (delete [this table id] [this table id opts]
    "Delete record by ID with optional admin context
    Returns: {:success? boolean}")
  (exists? [this table field value]
    "Check if record exists with field value
    Returns: boolean")
  (execute! [this sql params]
    "Execute raw SQL query with parameters
    Returns: result of query as returned by next.jdbc/execute!"))

(defprotocol TransactionManager
  "Database transaction management"
  (with-transaction [this f]
    "Execute function within database transaction
    Returns: result of function or throws on rollback")
  (rollback [this]
    "Rollback current transaction")
  (commit [this]
    "Commit current transaction"))
