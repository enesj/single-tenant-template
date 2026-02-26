(ns app.template.backend.utils.query-builders
  "Centralized query building utilities for admin services.

   This namespace provides reusable query builders that eliminate
   duplication across admin services while maintaining consistent
   patterns for pagination, filtering, sorting, and text search.

   Key features:
   - HoneySQL-based query composition
   - Automatic column name conversion (kebab-case to snake_case)
   - Type validation and null handling
   - Integration with existing database adapter
   - Audit context support"
  (:require
    [app.shared.pagination :as pagination]
    [app.shared.query-builders :as shared-qb]
    [clojure.string :as str]))

;; ============================================================================
;; Column and Table Utilities
;; ============================================================================

(defn kebab->snake
  "Convert kebab-case to snake_case for database columns."
  [s]
  (when s
    (str/replace (name s) "-" "_")))

(defn qualify-column
  "Qualify a column with table alias if provided."
  [column table-alias]
  (if table-alias
    (keyword (str (name table-alias) "." (kebab->snake column)))
    (keyword (kebab->snake column))))

;; ============================================================================
;; Pagination Builder
;; ============================================================================

(defn add-pagination
  "Add pagination clauses to a HoneySQL query.

   Options:
   - limit: Number of records to return (default: 50, max: 1000)
   - offset: Number of records to skip (default: 0)

   Example:
   (add-pagination query {:limit 25 :offset 50})"
  [query {:keys [limit offset]}]
  (shared-qb/apply-pagination query {:limit limit :offset offset}
    {:default-limit pagination/default-page-size
     :max-limit pagination/max-page-size
     :default-offset 0}))

;; ============================================================================
;; Sorting Builder
;; ============================================================================

(defn add-sorting
  "Add ORDER BY clause to a HoneySQL query with column conversion.

   Options:
   - sort-by: Column name (kebab-case, converted to snake_case)
   - sort-order: :asc or :desc (default: :desc)
   - table-alias: Optional table alias for column qualification
   - default-column: Fallback column (default: :created_at)

   Example:
   (add-sorting query {:sort-by \"full-name\"
                       :sort-order :asc
                       :table-alias :u})"
  [query {:keys [sort-by sort-order table-alias default-column]
          :or {sort-order :desc
               default-column :created_at}}]
  (let [column (or sort-by default-column)
        qualified-column (qualify-column column table-alias)
        order (shared-qb/normalize-order-direction sort-order {:default :desc})]
    (shared-qb/apply-order-by query qualified-column order)))

;; ============================================================================
;; Text Search Builder
;; ============================================================================

(defn build-text-search
  "Build a text search condition using ILIKE across multiple columns.

   Parameters:
   - search: Search term (will be wrapped with %)
   - columns: Vector of column keywords (with optional table aliases)

   Returns HoneySQL condition or nil if no search term.

   Example:
   (build-text-search \"john\" [:u/email :u/full_name])
   ; => [:or [:ilike :u/email \"%john%\"] [:ilike :u/full_name \"%john%\"]]"
  [search columns]
  (when (and search (not (str/blank? search)))
    (let [pattern (str "%" (str/trim search) "%")]
      (shared-qb/build-ilike-or columns pattern))))

;; ============================================================================
;; Filter Builder
;; ============================================================================

(defn build-filter-condition
  "Build a single filter condition based on type and value.

   Supported filter types:
   - :equal - Direct equality ([:= column value])
   - :in - IN clause ([:in column values])
   - :date-range - Date range with :from and :to keys
   - :boolean - Boolean check with some? validation
   - :like - Case-insensitive LIKE pattern

   Example:
   (build-filter-condition :status \"active\" :equal :u/status)
   ; => [:= :u/status \"active\"]"
  [value filter-type column]
  (case filter-type
    :equal
    (when value [:= column value])

    :in
    (when (seq value) [:in column value])

    :date-range
    (when (map? value)
      (let [{:keys [from to]} value
            conditions (cond-> []
                         from (conj [:>= column from])
                         to (conj [:<= column to]))]
        (when (seq conditions)
          (into [:and] conditions))))

    :boolean
    (when (some? value) [:= column value])

    :like
    (when (and value (not (str/blank? value)))
      [:ilike column (str "%" value "%")])

    nil))

(defn build-filters
  "Build WHERE conditions from a filter specification map.

   Filter spec format:
   {:column-name {:type :filter-type :value actual-value :table-alias :optional}}

   Example:
   (build-filters {:status {:type :equal :value \"active\" :table-alias :u}
                   :created-at {:type :date-range
                               :value {:from start-date :to end-date}
                               :table-alias :u}
                   :email-verified {:type :boolean :value true :table-alias :u}})

   Returns vector of conditions suitable for HoneySQL :where clause."
  [filter-spec]
  (let [conditions (keep (fn [[column-key {:keys [type value table-alias]}]]
                           (let [qualified-column (if table-alias
                                                    (keyword (str (name table-alias) "." (kebab->snake column-key)))
                                                    (keyword (kebab->snake column-key)))]
                             (build-filter-condition value type qualified-column)))
                     filter-spec)]
    (when (seq conditions)
      (into [:and] conditions))))

;; ============================================================================
;; Query Composer
;; ============================================================================

(defn compose-admin-query
  "Compose a complete admin query with all builders applied.

   Parameters:
   - base-query: Base HoneySQL query map
   - options: Map containing:
     - :search - Text search options {:term string :columns [keywords]}
     - :filters - Filter specification map
     - :sort - Sort options {:by column :order :asc/:desc :table-alias keyword :default keyword}
     - :pagination - Pagination options {:limit int :offset int}

   Example:
   (compose-admin-query
     {:select [:u.* :t.name] :from [[:users :u]] :join [[:tenants :t] [:= :u.tenant_id :t.id]]}
     {:search {:term \"john\" :columns [:u/email :u/full_name]}
      :filters {:status {:type :equal :value \"active\" :table-alias :u}
                :tenant-id {:type :equal :value 123 :table-alias :u}}
      :sort {:by :created-at :order :desc :table-alias :u}
      :pagination {:limit 25 :offset 50}})"
  [base-query {:keys [search filters sort pagination]}]
  (let [;; Build search condition
        search-condition (when search
                           (build-text-search (:term search) (:columns search)))

        ;; Build filter conditions
        filter-conditions (when filters
                            (build-filters filters))

        ;; Combine all WHERE conditions
        all-conditions (cond-> []
                         search-condition (conj search-condition)
                         filter-conditions (conj filter-conditions))

        ;; Apply conditions to query
        query-with-where (if (seq all-conditions)
                           (assoc base-query :where (into [:and] all-conditions))
                           base-query)

        ;; Apply sorting
        query-with-sorting (if sort
                             (add-sorting query-with-where sort)
                             query-with-where)

        ;; Apply pagination
        final-query (if pagination
                      (add-pagination query-with-sorting pagination)
                      query-with-sorting)]

    final-query))

;; ============================================================================
;; Convenience Functions for Common Patterns
;; ============================================================================

;; ============================================================================
;; Integration Helpers
;; ============================================================================


