(ns app.shared.query-builders
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
  (let [safe-limit (-> limit
                     (or pagination/default-page-size)
                     (min pagination/max-page-size)
                     (max 1))
        safe-offset (-> offset
                      (or 0)
                      (max 0))]
    (assoc query
      :limit safe-limit
      :offset safe-offset)))

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
        order (if (= sort-order :asc) :asc :desc)]
    (assoc query :order-by [[qualified-column order]])))

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
    (let [search-term (str "%" (str/trim search) "%")]
      (into [:or]
        (map (fn [col]
               [:ilike col search-term])
          columns)))))

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

(defn admin-list-query
  "Build a standard admin list query with common patterns.

   This is a convenience function for the most common admin query pattern:
   listing entities with search, filters, sorting, and pagination.

   Parameters:
   - table: Main table keyword (e.g., :users)
   - table-alias: Alias for main table (e.g., :u)
   - joins: Optional join clauses
   - search-columns: Columns to search in (e.g., [:u/email :u/full_name])
   - options: Same as compose-admin-query options

   Example:
   (admin-list-query :users :u
     [[:tenants :t] [:= :u.tenant_id :t.id]]
     [:u/email :u/full_name]
     {:search {:term \"john\"}
      :filters {:status {:type :equal :value \"active\"}}
      :sort {:by :created-at}
      :pagination {:limit 25}})"
  [table table-alias joins search-columns options]
  (let [base-select [(keyword (str (name table-alias) ".*"))]
        base-query (cond-> {:select base-select
                            :from [[table table-alias]]}
                     joins (assoc :join joins))

        ;; Enhance options with search columns if search term provided
        enhanced-options (if (and (:search options) search-columns)
                           (assoc-in options [:search :columns] search-columns)
                           options)]

    (compose-admin-query base-query enhanced-options)))

;; ============================================================================
;; Integration Helpers
;; ============================================================================

(defn extract-query-params
  "Extract query parameters from request params into structured format.

   Converts flat request parameters into the structured format expected
   by compose-admin-query. Handles common parameter naming conventions.

   Example:
   (extract-query-params {:search \"john\"
                         :status \"active\"
                         :tenant-id \"123\"
                         :sort-by \"created-at\"
                         :sort-order \"desc\"
                         :limit \"25\"
                         :offset \"50\"})
   ; => {:search {:term \"john\"}
   ;     :filters {:status {:type :equal :value \"active\"}
   ;               :tenant-id {:type :equal :value 123}}
   ;     :sort {:by :created-at :order :desc}
   ;     :pagination {:limit 25 :offset 50}}"
  [params]
  (let [{:keys [search sort-by sort-order limit offset]
         :or {sort-order "desc"}} params

        ;; Extract filter parameters (anything not in special keys)
        special-keys #{:search :sort-by :sort-order :limit :offset}
        filter-params (apply dissoc params special-keys)

        ;; Build result map
        result (cond-> {}
                 search (assoc :search {:term search})

                 (seq filter-params)
                 (assoc :filters
                   (into {} (map (fn [[k v]]
                                   [k {:type :equal :value v}])
                              filter-params)))

                 sort-by (assoc :sort {:by (keyword sort-by)
                                       :order (keyword sort-order)})

                 (or limit offset)
                 (assoc :pagination (cond-> {}
                                      limit (assoc :limit (parse-long (str limit)))
                                      offset (assoc :offset (parse-long (str offset))))))]
    result))
