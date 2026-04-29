(ns app.domain.backend.expenses.services.duplicates
  "Duplicate detection across canonical entities (Suppliers, Articles, Stores, Manufacturers, Subcategories).

  Strategies:
  - :exact       — Group by identical normalized_key values (no threshold, always precise).
  - :prefix      — Group by first N hyphen-tokens of normalized_key (in-memory).
  - :trigram     — SQL self-join using pg_trgm similarity() (default threshold 0.6).
  - :levenshtein — SQL self-join using fuzzystrmatch levenshtein() (default max-distance 1).

  Trigram and Levenshtein use Union-Find (single-linkage) clustering, which can chain
  transitively. Use max-cluster-size to cap runaway clusters caused by shared tokens
  (e.g. 'sarajevo', '71000') in address-based normalized keys.

  Each strategy returns clusters: vectors of entity maps belonging together."
  (:require
    [app.domain.backend.expenses.services.duplicates.config :as dup-config]
    [app.domain.backend.expenses.services.duplicates.context :as dup-context]
    [app.domain.backend.expenses.services.duplicates.similarity :as dup-sim]
    [buddy.core.codecs :as codecs]
    [buddy.core.hash :as hash]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

;; ============================================================================
;; Entity Configuration
;; ============================================================================

(def ^:private default-prefix-fetch-limit
  dup-config/default-prefix-fetch-limit)

(defn- normalize-fetch-limit
  [fetch-limit]
  (dup-config/normalize-fetch-limit fetch-limit))

(defn- get-entity-config!
  [entity-type]
  (dup-config/get-entity-config! entity-type))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- detection-key-col
  [{:keys [key-col]}]
  (or key-col :normalized_key))

(defn- in-memory-detection?
  [{:keys [normalize-fn key-col]}]
  (or (some? normalize-fn)
    (nil? key-col)))

(defn- prepare-detection-row
  [{:keys [name-col key-col normalize-fn] :as config} row]
  (let [detection-col (detection-key-col config)
        detection-key (or (get row detection-col)
                        (when key-col (get row key-col))
                        (when normalize-fn
                          (some-> (get row name-col) normalize-fn)))]
    (cond-> (assoc row :normalized_key detection-key)
      (keyword? detection-col) (assoc detection-col detection-key))))

(defn- detection-select-cols
  [{:keys [name-col key-col group-col display-cols]}]
  (cond-> [:id name-col :created_at]
    key-col (conj key-col)
    group-col (conj group-col)
    (seq display-cols) (into display-cols)))

(defn- fetch-all-rows
  "Fetch rows for an entity (id, name-col, detection key, and group-col when present).
  `fetch-limit` is always normalized and bounded to avoid unbounded scans."
  [db config fetch-limit]
  (let [fetch-limit* (normalize-fetch-limit fetch-limit)
        select-cols  (detection-select-cols config)]
    (->> (jdbc/execute!
           db
           (sql/format {:select   select-cols
                        :from     [(keyword (:table config))]
                        :order-by [[:created_at :asc]]
                        :limit    fetch-limit*})
           {:builder-fn rs/as-unqualified-lower-maps})
      (mapv #(prepare-detection-row config %)))))

(defn- prefix-tokens
  "Extract first `n` hyphen-delimited tokens from a normalized key."
  [normalized-key n]
  (when (and normalized-key (pos? n))
    (let [tokens (str/split normalized-key #"-")]
      (when (>= (count tokens) n)
        (str/join "-" (take n tokens))))))

(defn cluster-id
  "Compute a deterministic signature for a detected cluster.

  The ID is based on:
  - entity-type (keyword, e.g. :suppliers)
  - sorted member UUIDs (as lowercase strings)

  This intentionally hides *exactly* that set of members. If membership changes,
  the resulting cluster-id changes too (acceptable for MVP)."
  [entity-type members]
  (let [entity-part (some-> entity-type name)
        member-ids (->> members
                     (map #(or (:id %) %))
                     (keep identity)
                     (map str)
                     (map str/lower-case)
                     sort)
        payload (when (and (seq entity-part) (seq member-ids))
                  (str entity-part ":" (str/join "," member-ids)))]
    (when payload
      (-> payload
        (.getBytes "UTF-8")
        hash/sha256
        codecs/bytes->hex))))

(defn attach-cluster-ids
  "Attach `:cluster-id` to each cluster map."
  [entity-type clusters]
  (mapv (fn [cluster]
          (let [cid (cluster-id entity-type (:members cluster))]
            (cond-> cluster
              cid (assoc :cluster-id cid))))
    clusters))

(defn filter-ignored-clusters
  "Remove clusters whose `:cluster-id` exists in `ignored-ids` (a set)."
  [ignored-ids clusters]
  (if (seq ignored-ids)
    (->> clusters
      (remove (fn [cluster]
                (contains? ignored-ids (or (:cluster-id cluster) (:cluster_id cluster)))))
      vec)
    (vec clusters)))

;; ============================================================================
;; Union-Find for Clustering Pairs
;; ============================================================================

;; ============================================================================
;; Strategy: Prefix Grouping
;; ============================================================================

(defn- fetch-rows-by-ids
  [db config ids]
  (when (seq ids)
    (->> (jdbc/execute!
           db
           (sql/format {:select   (detection-select-cols config)
                        :from     [(keyword (:table config))]
                        :where    [:in :id ids]
                        :order-by [[:created_at :asc]]})
           {:builder-fn rs/as-unqualified-lower-maps})
      (mapv #(prepare-detection-row config %)))))

(defn- clusters-from-pairs
  [rows pairs opts]
  (dup-sim/clusters-from-pairs rows pairs opts))

(defn- trigram-similarity
  [a b]
  (dup-sim/trigram-similarity a b))

(defn- levenshtein-distance
  [a b]
  (dup-sim/levenshtein-distance a b))

(defn- detect-similar-pairs-in-memory
  [rows group-col score-fn matches?]
  (dup-sim/detect-similar-pairs-in-memory rows group-col score-fn matches?))

(defn detect-prefix-duplicates
  "Group entities by first N hyphen-tokens of normalized_key.

  When the entity config has :group-col, only rows sharing the same group
  value are compared (e.g. stores within the same supplier).

  Options:
  - :prefix-words (default 2) — number of leading tokens to group by
  - :limit (default 50) — max clusters to return
  - :fetch-limit (default 5000) — max rows fetched, bounded to [1, 20000]"
  [db entity-type {:keys [prefix-words limit fetch-limit]
                   :or {prefix-words 2 limit 50 fetch-limit default-prefix-fetch-limit}}]
  (let [config    (get-entity-config! entity-type)
        rows      (fetch-all-rows db config fetch-limit)
        group-col (:group-col config)
        grouped   (->> rows
                    (filter :normalized_key)
                    (group-by (fn [row]
                                [(prefix-tokens (:normalized_key row) prefix-words)
                                 (when group-col (get row group-col))]))
                    (remove (fn [[[prefix _]]] (nil? prefix)))
                    (filter (fn [[_ members]] (> (count members) 1)))
                    (sort-by (fn [[_ members]] (- (count members))))
                    (take limit))]
    (mapv (fn [[_ members]]
            {:members (vec members)
             :count   (count members)})
      grouped)))

;; ============================================================================
;; Strategy: Trigram Similarity
;; ============================================================================

(defn detect-trigram-duplicates
  "Find duplicates using pg_trgm similarity() via SQL self-join.

  When the entity config has :group-col, only pairs sharing the same group
  value are compared (e.g. stores within the same supplier).

  Options:
  - :threshold (default 0.6) — minimum similarity score; lower values cause
    more transitive chaining via Union-Find (the 'megacluster' problem)
  - :limit (default 50) — max clusters to return
  - :max-cluster-size (default 10) — discard clusters larger than this;
    giant clusters almost always result from shared address tokens, not real duplicates"
  [db entity-type {:keys [threshold limit max-cluster-size fetch-limit]
                   :or {threshold 0.6 limit 50 max-cluster-size 10
                        fetch-limit default-prefix-fetch-limit}}]
  (let [config (get-entity-config! entity-type)]
    (if (in-memory-detection? config)
      (let [rows (fetch-all-rows db config fetch-limit)
            pairs (detect-similar-pairs-in-memory rows (:group-col config) trigram-similarity #(> % threshold))]
        (clusters-from-pairs rows pairs {:limit limit :max-cluster-size max-cluster-size}))
      (let [table (keyword (:table config))
            group-col (:group-col config)
            key-col (:key-col config)
            group-condition (when group-col
                              [:= (keyword (str "a." (name group-col)))
                               (keyword (str "b." (name group-col)))])
            join-cond (cond-> [:and
                               [:< :a.id :b.id]
                               [:> [:similarity (keyword (str "a." (name key-col)))
                                    (keyword (str "b." (name key-col)))] threshold]]
                        group-condition (conj group-condition))
            pairs (jdbc/execute!
                    db
                    (sql/format
                      {:select [[:a.id :id_a]
                                [:b.id :id_b]
                                [[:similarity (keyword (str "a." (name key-col)))
                                  (keyword (str "b." (name key-col)))] :sim]]
                       :from [[table :a]]
                       :join [[table :b] join-cond]
                       :order-by [[:sim :desc]]
                       :limit (* limit 10)})
                    {:builder-fn rs/as-unqualified-lower-maps})
            rows (fetch-rows-by-ids db config (distinct (concat (map :id_a pairs) (map :id_b pairs))))]
        (clusters-from-pairs rows pairs {:limit limit :max-cluster-size max-cluster-size})))))

;; ============================================================================
;; Strategy: Levenshtein Distance
;; ============================================================================

(defn detect-levenshtein-duplicates
  "Find duplicates using fuzzystrmatch levenshtein() via SQL self-join.

  When the entity config has :group-col, only pairs sharing the same group
  value are compared (e.g. stores within the same supplier).

  Options:
  - :max-distance (default 1) — maximum edit distance; 2 is often too loose for
    short keys like 'pj-3' vs 'pj-57' (distance 2, but unrelated stores)
  - :limit (default 50) — max clusters to return
  - :max-cluster-size (default 10) — discard clusters larger than this"
  [db entity-type {:keys [max-distance limit max-cluster-size fetch-limit]
                   :or {max-distance 1 limit 50 max-cluster-size 10
                        fetch-limit default-prefix-fetch-limit}}]
  (let [config (get-entity-config! entity-type)]
    (if (in-memory-detection? config)
      (let [rows (fetch-all-rows db config fetch-limit)
            pairs (detect-similar-pairs-in-memory rows (:group-col config) levenshtein-distance #(<= % max-distance))]
        (clusters-from-pairs rows pairs {:limit limit :max-cluster-size max-cluster-size}))
      (let [table (keyword (:table config))
            group-col (:group-col config)
            key-col (:key-col config)
            group-condition (when group-col
                              [:= (keyword (str "a." (name group-col)))
                               (keyword (str "b." (name group-col)))])
            join-cond (cond-> [:and
                               [:< :a.id :b.id]
                               [:<= [:levenshtein (keyword (str "a." (name key-col)))
                                     (keyword (str "b." (name key-col)))] max-distance]]
                        group-condition (conj group-condition))
            pairs (jdbc/execute!
                    db
                    (sql/format
                      {:select [[:a.id :id_a]
                                [:b.id :id_b]
                                [[:levenshtein (keyword (str "a." (name key-col)))
                                  (keyword (str "b." (name key-col)))] :dist]]
                       :from [[table :a]]
                       :join [[table :b] join-cond]
                       :order-by [[:dist :asc]]
                       :limit (* limit 10)})
                    {:builder-fn rs/as-unqualified-lower-maps})
            rows (fetch-rows-by-ids db config (distinct (concat (map :id_a pairs) (map :id_b pairs))))]
        (clusters-from-pairs rows pairs {:limit limit :max-cluster-size max-cluster-size})))))

;; ============================================================================
;; Dispatcher
;; ============================================================================

;; ============================================================================
;; Strategy: Exact Key Match
;; ============================================================================

(defn detect-exact-duplicates
  "Group entities that share an identical normalized_key value.

  When the entity config has :group-col, the grouping is scoped per group
  (e.g. stores: same normalized_key AND same supplier_id).

  No threshold to tune — any exact match is a real candidate for merging.
  Particularly useful for Stores, where OCR extracts the same address string
  for different receipts from the same branch.

  Options:
  - :limit (default 50) — max clusters to return"
  [db entity-type {:keys [limit fetch-limit]
                   :or {limit 50 fetch-limit default-prefix-fetch-limit}}]
  (let [config (get-entity-config! entity-type)]
    (if (in-memory-detection? config)
      (let [group-col (:group-col config)
            rows (fetch-all-rows db config fetch-limit)]
        (->> rows
          (filter :normalized_key)
          (group-by (fn [row]
                      [(:normalized_key row)
                       (when group-col (get row group-col))]))
          (filter (fn [[_ members]] (> (count members) 1)))
          (mapv (fn [[_ members]]
                  {:members (vec members)
                   :count (count members)}))
          (sort-by #(- (:count %)))
          (take limit)
          vec))
      (let [table (keyword (:table config))
            key-col (:key-col config)
            group-col (:group-col config)
            group-by-cols (cond-> [key-col] group-col (conj group-col))
            select-cols (cond-> [key-col [[:count :*] :cnt]] group-col (conj group-col))
            dupes (jdbc/execute!
                    db
                    (sql/format {:select select-cols
                                 :from [table]
                                 :where [:is-not key-col nil]
                                 :group-by group-by-cols
                                 :having [:> [:count :*] 1]
                                 :order-by [[:cnt :desc]]
                                 :limit limit})
                    {:builder-fn rs/as-unqualified-lower-maps})]
        (if (empty? dupes)
          []
          (let [dupe-keys (mapv key-col dupes)
                rows (->> (jdbc/execute!
                            db
                            (sql/format {:select (detection-select-cols config)
                                         :from [table]
                                         :where [:in key-col dupe-keys]
                                         :order-by [key-col [:created_at :asc]]})
                            {:builder-fn rs/as-unqualified-lower-maps})
                       (mapv #(prepare-detection-row config %)))
                group-fn (if group-col
                           (juxt key-col group-col)
                           key-col)]
            (->> rows
              (group-by group-fn)
              (filter (fn [[_ members]] (> (count members) 1)))
              (mapv (fn [[_ members]]
                      {:members (vec members)
                       :count (count members)}))
              (sort-by #(- (:count %)))
              (take limit)
              vec)))))))

(defn detect-duplicates
  "Detect duplicates for an entity type using the specified strategy.

  Strategy must be one of :exact, :prefix, :trigram, :levenshtein.
  Options are strategy-specific (see individual functions)."
  [db entity-type strategy opts]
  (case strategy
    :exact       (detect-exact-duplicates db entity-type opts)
    :prefix      (detect-prefix-duplicates db entity-type opts)
    :trigram     (detect-trigram-duplicates db entity-type opts)
    :levenshtein (detect-levenshtein-duplicates db entity-type opts)
    (throw (ex-info (str "Unknown strategy: " strategy)
             {:strategy         strategy
              :valid-strategies [:exact :prefix :trigram :levenshtein]}))))

;; ============================================================================
;; Usage Count Enrichment
;; ============================================================================

(defn enrich-members-with-context
  "Attach entity-specific display context to standalone candidate rows."
  [db entity-type members]
  (dup-context/enrich-members-with-context db entity-type members))

(defn enrich-with-usage-counts
  "For each member in each cluster, sum FK reference counts across referencing tables.

  Adds :usage-count and any entity-specific display context to each member map."
  [db entity-type clusters]
  (dup-context/enrich-with-usage-counts db entity-type clusters))

(defn filter-article-clusters-with-distinct-manufacturers
  "Remove article clusters when every member has a known, distinct manufacturer.

  These usually represent same product types across different brands, not true
  merge candidates. Clusters with any missing manufacturer stay visible so admins
  can still review genuinely ambiguous cases."
  [entity-type clusters]
  (dup-context/filter-article-clusters-with-distinct-manufacturers entity-type clusters))

(comment
  ;; REPL usage examples
  ;; (detect-duplicates db :suppliers :prefix {:prefix-words 2})
  ;; (detect-duplicates db :articles :trigram {:threshold 0.5})
  ;; (detect-duplicates db :manufacturers :levenshtein {:max-distance 2})
  ;; (enrich-with-usage-counts db :suppliers clusters)
  :rcf)
