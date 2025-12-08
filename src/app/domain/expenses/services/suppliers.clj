(ns app.domain.expenses.services.suppliers
  "Supplier CRUD and normalization services for expense tracking"
  (:require
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Normalization
;; ============================================================================

(defn normalize-supplier-key
  "Normalize supplier name to lowercase, replace spaces with hyphens, remove special chars.
   Used for deduplication and fuzzy matching.
   
   Examples:
     'Bingo Centar' -> 'bingo-centar'
     'DM Drogerie!' -> 'dm-drogerie'
     '  KONZUM  ' -> 'konzum'"
  [name]
  (when name
    (-> name
        str/trim
        str/lower-case
        (str/replace #"[^a-z0-9\s-]" "")  ; Remove special chars
        (str/replace #"\s+" "-"))))        ; Spaces to hyphens

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-supplier!
  "Create a new supplier with normalized key for deduplication.
   
   Args:
     db - Database connection
     data - Map with :display_name (required), :address, :tax_id (optional)
   
   Returns: Created supplier record with generated :id"
  [db {:keys [display_name address tax_id] :as data}]
  (when-not display_name
    (throw (ex-info "display_name is required" {:data data})))
  
  (let [normalized-key (normalize-supplier-key display_name)
        supplier-data {:id (UUID/randomUUID)
                       :display_name display_name
                       :normalized_key normalized-key
                       :address address
                       :tax_id tax_id}
        insert-sql (sql/format {:insert-into :suppliers
                                :values [supplier-data]
                                :returning [:*]})]
    (jdbc/execute-one! db insert-sql {:builder-fn rs/as-unqualified-lower-maps})))

(defn get-supplier
  "Get supplier by ID.
   
   Args:
     db - Database connection
     id - Supplier UUID
   
   Returns: Supplier map or nil if not found"
  [db id]
  (let [query (sql/format {:select [:*]
                           :from [:suppliers]
                           :where [:= :id id]})]
    (jdbc/execute-one! db query {:builder-fn rs/as-unqualified-lower-maps})))

(defn list-suppliers
  "List suppliers with optional search and pagination.
   
   Args:
     db - Database connection
     opts - Map with optional keys:
       :search - Text to search in display_name or normalized_key
       :limit - Max results (default 100)
       :offset - Offset for pagination (default 0)
       :order-by - Column to order by (default :display_name)
       :order-dir - :asc or :desc (default :asc)
   
   Returns: Vector of supplier maps"
  [db {:keys [search limit offset order-by order-dir]
       :or {limit 100 offset 0 order-by :display_name order-dir :asc}}]
  (let [base-query {:select [:*]
                    :from [:suppliers]}
        query (cond-> base-query
                search
                (assoc :where [:or
                               [:ilike :display_name (str "%" search "%")]
                               [:ilike :normalized_key (str "%" search "%")]])
                
                true
                (assoc :order-by [[order-by order-dir]])
                
                limit
                (assoc :limit limit)
                
                offset
                (assoc :offset offset))
        sql-query (sql/format query)]
    (jdbc/execute! db sql-query {:builder-fn rs/as-unqualified-lower-maps})))

(defn update-supplier!
  "Update supplier fields.
   
   Args:
     db - Database connection
     id - Supplier UUID
     updates - Map of fields to update (can include :display_name, :address, :tax_id)
   
   Returns: Updated supplier map or nil if not found"
  [db id updates]
  (let [;; Recalculate normalized_key if display_name changed
        updates-with-key (if (:display_name updates)
                           (assoc updates :normalized_key 
                                  (normalize-supplier-key (:display_name updates)))
                           updates)
        ;; Add updated_at timestamp
        updates-final (assoc updates-with-key :updated_at [:now])
        update-sql (sql/format {:update :suppliers
                                :set updates-final
                                :where [:= :id id]
                                :returning [:*]})]
    (jdbc/execute-one! db update-sql {:builder-fn rs/as-unqualified-lower-maps})))

(defn delete-supplier!
  "Delete a supplier by ID.
   WARNING: This will fail if supplier is referenced by expenses.
   
   Args:
     db - Database connection
     id - Supplier UUID
   
   Returns: Boolean indicating success"
  [db id]
  (let [delete-sql (sql/format {:delete-from :suppliers
                                :where [:= :id id]})]
    (pos? (::jdbc/update-count (jdbc/execute-one! db delete-sql)))))

;; ============================================================================
;; Advanced Queries
;; ============================================================================

(defn find-by-normalized-key
  "Find supplier by normalized key (exact match).
   Useful for deduplication.
   
   Args:
     db - Database connection
     name - Display name to normalize and search
   
   Returns: Supplier map or nil"
  [db name]
  (let [normalized (normalize-supplier-key name)
        query (sql/format {:select [:*]
                           :from [:suppliers]
                           :where [:= :normalized_key normalized]})]
    (jdbc/execute-one! db query {:builder-fn rs/as-unqualified-lower-maps})))

(defn find-or-create-supplier!
  "Find supplier by normalized key or create if not exists.
   Prevents duplicate suppliers with minor name variations.
   
   Args:
     db - Database connection
     name - Display name to search/create
     extra-data - Optional map with :address, :tax_id for creation
   
   Returns: Existing or newly created supplier map"
  [db name extra-data]
  (if-let [existing (find-by-normalized-key db name)]
    existing
    (create-supplier! db (merge {:display_name name} extra-data))))

(defn search-suppliers
  "Search suppliers for autocomplete.
   Returns top N matches sorted by relevance.
   
   Args:
     db - Database connection
     query - Search text
     opts - Map with optional :limit (default 10)
   
   Returns: Vector of supplier maps"
  [db query {:keys [limit] :or {limit 10}}]
  (when (and query (>= (count query) 2))  ; Minimum 2 chars
    (let [search-pattern (str "%" query "%")
          sql-query (sql/format {:select [:*]
                                 :from [:suppliers]
                                 :where [:or
                                         [:ilike :display_name search-pattern]
                                         [:ilike :normalized_key search-pattern]]
                                 :order-by [[:display_name :asc]]
                                 :limit limit})]
      (jdbc/execute! db sql-query {:builder-fn rs/as-unqualified-lower-maps}))))

(defn count-suppliers
  "Count total suppliers, optionally with search filter.
   
   Args:
     db - Database connection
     search - Optional search text
   
   Returns: Integer count"
  [db & [search]]
  (let [base-query {:select [[[:count :*] :total]]
                    :from [:suppliers]}
        query (if search
                (assoc base-query :where [:or
                                          [:ilike :display_name (str "%" search "%")]
                                          [:ilike :normalized_key (str "%" search "%")]])
                base-query)
        sql-query (sql/format query)
        result (jdbc/execute-one! db sql-query {:builder-fn rs/as-unqualified-lower-maps})]
    (:total result)))
