(ns app.shared.query-builders
  "Shared (backend) HoneySQL query building helpers.

   These helpers are intentionally small and pure so they can be used across
   template admin services and domain services without forcing a single
   opinionated query-builder abstraction.

   NOTE: This namespace is CLJ-only (not .cljc) because it targets backend
   HoneySQL query maps.")

;; -----------------------------------------------------------------------------
;; Pagination helpers
;; -----------------------------------------------------------------------------

(defn normalize-limit
  "Normalize a limit value.

   Options:
   - :default  Default value when limit is nil
   - :min      Minimum allowed limit (default 1)
   - :max      Maximum allowed limit (when provided)

   Returns an integer (or nil if both limit and :default are nil)."
  ([limit]
   (normalize-limit limit {}))
  ([limit {:keys [default min max]
           :or {min 1}}]
   (when-some [v (or limit default)]
     (let [v (clojure.core/max min (long v))]
       (if (some? max)
         (clojure.core/min (long max) v)
         v)))))

(defn normalize-offset
  "Normalize an offset value.

   Options:
   - :default  Default value when offset is nil (default 0)
   - :min      Minimum allowed offset (default 0)

   Returns an integer."
  ([offset]
   (normalize-offset offset {}))
  ([offset {:keys [default min]
            :or {default 0
                 min 0}}]
   (clojure.core/max (long min) (long (or offset default)))))

(defn apply-pagination
  "Apply limit/offset to a HoneySQL query map.

   `pagination` is a map with optional keys :limit and :offset.

   `opts` controls normalization:
   - :default-limit
   - :max-limit
   - :default-offset

   If :limit is nil and :default-limit is nil, :limit will not be assoc'd."
  ([query pagination]
   (apply-pagination query pagination {}))
  ([query {:keys [limit offset]} {:keys [default-limit max-limit default-offset]
                                  :or {default-offset 0}}]
   (let [limit* (normalize-limit limit {:default default-limit :max max-limit})
         offset* (normalize-offset offset {:default default-offset})]
     (cond-> query
       (some? limit*) (assoc :limit limit*)
       (some? offset*) (assoc :offset offset*)))))

;; -----------------------------------------------------------------------------
;; Sorting helpers
;; -----------------------------------------------------------------------------

(defn normalize-order-direction
  "Normalize an order direction input to :asc or :desc.

  Accepts keywords or strings. Anything other than :asc/\"asc\" becomes :desc by
   default unless a custom :default is provided."
  ([dir]
   (normalize-order-direction dir {}))
  ([dir {:keys [default]
         :or {default :desc}}]
   (cond
     (or (= dir :asc) (= dir "asc")) :asc
     (or (= dir :desc) (= dir "desc")) :desc
     :else default)))

(defn apply-order-by
  "Apply an ORDER BY clause to a HoneySQL query map.

   No-ops if `column` is nil."
  [query column direction]
  (if (some? column)
    (assoc query :order-by [[column direction]])
    query))

;; -----------------------------------------------------------------------------
;; Search helpers
;; -----------------------------------------------------------------------------

(defn build-ilike-or
  "Build a HoneySQL OR clause of ILIKE comparisons.

   Example:
   (build-ilike-or [:u/email :u/full_name] \"%john%\")
   => [:or [:ilike :u/email \"%john%\"] [:ilike :u/full_name \"%john%\"]]"
  [fields pattern]
  (into [:or]
    (map (fn [field] [:ilike field pattern]) fields)))

(defn merge-where-and
  "Merge an additional WHERE clause into an existing :where.

   Mirrors the common pattern used across the codebase:
   - if existing exists => [:and existing new]
   - else => new"
  [existing clause]
  (if existing
    [:and existing clause]
    clause))

(defn apply-search-where
  "Apply an ILIKE OR search clause to a HoneySQL query map.

  `pattern` should already include any desired wildcards (e.g. \"%term%\")
   and callers should decide how/if to trim/blank-check the term."
  [query search-fields pattern]
  (if (and pattern (seq search-fields))
    (let [search-where (build-ilike-or search-fields pattern)]
      (update query :where (fn [existing]
                             (merge-where-and existing search-where))))
    query))

;; -----------------------------------------------------------------------------
;; Per-column text filters
;; -----------------------------------------------------------------------------

(defn apply-text-filters
  "Apply per-column ILIKE text filters to a HoneySQL query map.

   `column-mapping` is a map from filter key (keyword) to SQL column (keyword):
     {:supplier-display-name :s.display_name
      :original-filename     :receipts.original_filename}

   `filters` is a map of filter values (same keys as column-mapping):
     {:supplier-display-name \"BINGO\"}

   Only non-blank string values produce WHERE conditions.

   Example:
     (apply-text-filters query
       {:supplier-display-name :s.display_name}
       {:supplier-display-name \"BINGO\"})
     ;; => adds [:ilike :s.display_name \"%BINGO%\"] to :where"
  [query column-mapping filters]
  (reduce-kv
    (fn [q filter-key sql-col]
      (let [v (get filters filter-key)]
        (if (and (string? v) (seq v))
          (update q :where merge-where-and
            [:ilike sql-col (str "%" v "%")])
          q)))
    query
    column-mapping))

(defn has-text-filters?
  "Returns true if any of the given filter keys have non-blank values in `filters`."
  [filter-keys filters]
  (some (fn [k] (seq (get filters k))) filter-keys))
