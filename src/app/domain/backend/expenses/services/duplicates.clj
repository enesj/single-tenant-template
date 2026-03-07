(ns app.domain.backend.expenses.services.duplicates
  "Duplicate detection across canonical entities (Suppliers, Articles, Stores, Manufacturers).

  Strategies:
  - :prefix   — Group by first N hyphen-tokens of normalized_key (in-memory).
  - :trigram  — SQL self-join using pg_trgm similarity().
  - :levenshtein — SQL self-join using fuzzystrmatch levenshtein().

  Each strategy returns clusters: vectors of entity maps belonging together."
  (:require
    [buddy.core.codecs :as codecs]
    [buddy.core.hash :as hash]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

;; ============================================================================
;; Entity Configuration
;; ============================================================================

(def ^:private entity-configs
  "Per-entity config for duplicate detection."
  {:suppliers     {:table "suppliers"
                   :name-col :display_name
                   :key-col :normalized_key
                   :fk-tables {:expenses {:col :supplier_id}
                               :stores {:col :supplier_id}
                               :supplier_aliases {:col :supplier_id}
                               :article_aliases {:col :supplier_id}}}
   :articles      {:table "articles"
                   :name-col :canonical_name
                   :key-col :normalized_key
                   :fk-tables {:expense_items {:col :article_id}
                               :article_aliases {:col :article_id}}}
   :stores        {:table "stores"
                   :name-col :display_name
                   :key-col :normalized_key
                   :fk-tables {:expenses {:col :store_id}
                               :store_aliases {:col :store_id}}}
   :manufacturers {:table "manufacturers"
                   :name-col :display_name
                   :key-col :normalized_key
                   :fk-tables {:articles {:col :manufacturer_id}}}})

(def ^:private default-prefix-fetch-limit
  5000)

(def ^:private max-prefix-fetch-limit
  20000)

(defn- normalize-fetch-limit
  "Normalize fetch-limit to a bounded positive integer.

  - nil -> default
  - values < 1 -> 1
  - values > max -> max"
  [fetch-limit]
  (-> (or fetch-limit default-prefix-fetch-limit)
    (max 1)
    (min max-prefix-fetch-limit)))

(defn- get-entity-config!
  [entity-type]
  (or (get entity-configs entity-type)
    (throw (ex-info (str "Unknown entity type: " entity-type)
             {:entity-type entity-type
              :valid-types (keys entity-configs)}))))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- fetch-all-rows
  "Fetch rows for an entity (id, name-col, key-col).
  `fetch-limit` is always normalized and bounded to avoid unbounded scans."
  [db {:keys [table name-col key-col]} fetch-limit]
  (let [fetch-limit* (normalize-fetch-limit fetch-limit)]
    (jdbc/execute!
      db
      (sql/format {:select [:id name-col key-col :created_at]
                   :from [(keyword table)]
                   :order-by [[:created_at :asc]]
                   :limit fetch-limit*})
      {:builder-fn rs/as-unqualified-lower-maps})))

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

(defn- make-union-find
  "Create a mutable union-find structure (atom of parent map)."
  [ids]
  (atom (zipmap ids ids)))

(defn- uf-find
  "Find root with path compression."
  [uf x]
  (let [parent (get @uf x x)]
    (if (= parent x)
      x
      (let [root (uf-find uf parent)]
        (swap! uf assoc x root)
        root))))

(defn- uf-union
  "Merge the sets containing x and y."
  [uf x y]
  (let [rx (uf-find uf x)
        ry (uf-find uf y)]
    (when (not= rx ry)
      (swap! uf assoc ry rx))))

(defn- uf-clusters
  "Return clusters (groups of >=2 items) from the union-find."
  [uf]
  (->> (keys @uf)
    (group-by #(uf-find uf %))
    vals
    (filter #(> (count %) 1))))

;; ============================================================================
;; Strategy: Prefix Grouping
;; ============================================================================

(defn detect-prefix-duplicates
  "Group entities by first N hyphen-tokens of normalized_key.

  Options:
  - :prefix-words (default 2) — number of leading tokens to group by
  - :limit (default 50) — max clusters to return
  - :fetch-limit (default 5000) — max rows fetched, bounded to [1, 20000]"
  [db entity-type {:keys [prefix-words limit fetch-limit]
                   :or {prefix-words 2 limit 50 fetch-limit default-prefix-fetch-limit}}]
  (let [config (get-entity-config! entity-type)
        rows (fetch-all-rows db config fetch-limit)
        key-col (:key-col config)
        grouped (->> rows
                  (filter #(get % key-col))
                  (group-by #(prefix-tokens (get % key-col) prefix-words))
                  (remove (fn [[k _]] (nil? k)))
                  (filter (fn [[_ members]] (> (count members) 1)))
                  (sort-by (fn [[_ members]] (- (count members))))
                  (take limit))]
    (mapv (fn [[_prefix members]]
            {:members (vec members)
             :count (count members)})
      grouped)))

;; ============================================================================
;; Strategy: Trigram Similarity
;; ============================================================================

(defn detect-trigram-duplicates
  "Find duplicates using pg_trgm similarity() via SQL self-join.

  Options:
  - :threshold (default 0.4) — minimum similarity score
  - :limit (default 50) — max clusters to return"
  [db entity-type {:keys [threshold limit]
                   :or {threshold 0.4 limit 50}}]
  (let [config (get-entity-config! entity-type)
        table (keyword (:table config))
        ;; Self-join: find pairs where similarity exceeds threshold
        pairs (jdbc/execute!
                db
                (sql/format
                  {:select [[:a.id :id_a] [:b.id :id_b]
                            [[:similarity :a.normalized_key :b.normalized_key] :sim]]
                   :from [[table :a]]
                   :join [[table :b] [:and
                                      [:< :a.id :b.id]
                                      [:> [:similarity :a.normalized_key :b.normalized_key]
                                       threshold]]]
                   :order-by [[:sim :desc]]
                   :limit (* limit 10)})
                {:builder-fn rs/as-unqualified-lower-maps})
        ;; Build clusters via union-find
        all-ids (distinct (concat (map :id_a pairs) (map :id_b pairs)))
        uf (make-union-find all-ids)]
    (doseq [{:keys [id_a id_b]} pairs]
      (uf-union uf id_a id_b))
    (let [clusters (uf-clusters uf)
          ;; Fetch full rows for clustered IDs
          clustered-ids (set (apply concat clusters))]
      (if (empty? clustered-ids)
        []
        (let [rows (jdbc/execute!
                     db
                     (sql/format {:select [:id (:name-col config) (:key-col config) :created_at]
                                  :from [table]
                                  :where [:in :id clustered-ids]})
                     {:builder-fn rs/as-unqualified-lower-maps})
              id->row (zipmap (map :id rows) rows)]
          (->> clusters
            (mapv (fn [ids]
                    {:members (mapv id->row ids)
                     :count (count ids)}))
            (sort-by #(- (:count %)))
            (take limit)
            vec))))))

;; ============================================================================
;; Strategy: Levenshtein Distance
;; ============================================================================

(defn detect-levenshtein-duplicates
  "Find duplicates using fuzzystrmatch levenshtein() via SQL self-join.

  Options:
  - :max-distance (default 2) — maximum edit distance
  - :limit (default 50) — max clusters to return"
  [db entity-type {:keys [max-distance limit]
                   :or {max-distance 2 limit 50}}]
  (let [config (get-entity-config! entity-type)
        table (keyword (:table config))
        pairs (jdbc/execute!
                db
                (sql/format
                  {:select [[:a.id :id_a] [:b.id :id_b]
                            [[:levenshtein :a.normalized_key :b.normalized_key] :dist]]
                   :from [[table :a]]
                   :join [[table :b] [:and
                                      [:< :a.id :b.id]
                                      [:<= [:levenshtein :a.normalized_key :b.normalized_key]
                                       max-distance]]]
                   :order-by [[:dist :asc]]
                   :limit (* limit 10)})
                {:builder-fn rs/as-unqualified-lower-maps})
        all-ids (distinct (concat (map :id_a pairs) (map :id_b pairs)))
        uf (make-union-find all-ids)]
    (doseq [{:keys [id_a id_b]} pairs]
      (uf-union uf id_a id_b))
    (let [clusters (uf-clusters uf)
          clustered-ids (set (apply concat clusters))]
      (if (empty? clustered-ids)
        []
        (let [rows (jdbc/execute!
                     db
                     (sql/format {:select [:id (:name-col config) (:key-col config) :created_at]
                                  :from [table]
                                  :where [:in :id clustered-ids]})
                     {:builder-fn rs/as-unqualified-lower-maps})
              id->row (zipmap (map :id rows) rows)]
          (->> clusters
            (mapv (fn [ids]
                    {:members (mapv id->row ids)
                     :count (count ids)}))
            (sort-by #(- (:count %)))
            (take limit)
            vec))))))

;; ============================================================================
;; Dispatcher
;; ============================================================================

(defn detect-duplicates
  "Detect duplicates for an entity type using the specified strategy.

  Strategy must be one of :prefix, :trigram, :levenshtein.
  Options are strategy-specific (see individual functions)."
  [db entity-type strategy opts]
  (case strategy
    :prefix (detect-prefix-duplicates db entity-type opts)
    :trigram (detect-trigram-duplicates db entity-type opts)
    :levenshtein (detect-levenshtein-duplicates db entity-type opts)
    (throw (ex-info (str "Unknown strategy: " strategy)
             {:strategy strategy
              :valid-strategies [:prefix :trigram :levenshtein]}))))

;; ============================================================================
;; Usage Count Enrichment
;; ============================================================================

(defn- derive-price-label
  [{:keys [unit_price qty line_total currency]}]
  (let [amount (cond
                 (some? unit_price) (bigdec unit_price)
                 (and (some? line_total)
                   (some? qty)
                   (not (zero? (bigdec qty))))
                 (.divide (bigdec line_total) (bigdec qty) 2 java.math.RoundingMode/HALF_UP)

                 (some? line_total) (bigdec line_total)
                 :else nil)]
    (when amount
      (str (.setScale amount 2 java.math.RoundingMode/HALF_UP)
        (when (seq (str currency))
          (str " " currency))))))

(defn- article-price-labels-by-id
  [db all-ids]
  (let [direct-rows (jdbc/execute!
                      db
                      (sql/format {:select [[:ei.article_id :entity_id]
                                            :ei.unit_price
                                            :ei.qty
                                            :ei.line_total
                                            [:e.currency :currency]]
                                   :from [[:expense_items :ei]]
                                   :left-join [[:expenses :e] [:= :e.id :ei.expense_id]]
                                   :where [:in :ei.article_id all-ids]})
                      {:builder-fn rs/as-unqualified-lower-maps})
        alias-rows (jdbc/execute!
                     db
                     (sql/format {:select [[:aa.article_id :entity_id]
                                           :ei.unit_price
                                           :ei.qty
                                           :ei.line_total
                                           [:e.currency :currency]]
                                  :from [[:article_aliases :aa]]
                                  :join [[:expense_items :ei] [:= :ei.alias_id :aa.id]]
                                  :left-join [[:expenses :e] [:= :e.id :ei.expense_id]]
                                  :where [:in :aa.article_id all-ids]})
                     {:builder-fn rs/as-unqualified-lower-maps})]
    (reduce
      (fn [acc {:keys [entity_id] :as row}]
        (if-let [label (derive-price-label row)]
          (update acc entity_id (fnil conj []) label)
          acc))
      {}
      (concat direct-rows alias-rows))))

(defn- store-supplier-names-by-id
  [db all-ids]
  (->> (jdbc/execute!
         db
         (sql/format {:select [[:st.id :entity_id]
                               [:s.display_name :supplier_display_name]]
                      :from [[:stores :st]]
                      :join [[:suppliers :s] [:= :s.id :st.supplier_id]]
                      :where [:in :st.id all-ids]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (reduce (fn [acc {:keys [entity_id supplier_display_name]}]
              (assoc acc entity_id {:supplier-display-name supplier_display_name}))
      {})))

(defn- contextual-info-by-id
  [db entity-type all-ids]
  (case entity-type
    :articles
    (->> (article-price-labels-by-id db all-ids)
      (reduce-kv (fn [acc entity-id labels]
                   (assoc acc entity-id {:price-labels (->> labels distinct sort vec)}))
        {}))

    :stores
    (store-supplier-names-by-id db all-ids)

    {}))

(defn enrich-with-usage-counts
  "For each member in each cluster, sum FK reference counts across referencing tables.

  Adds :usage-count and any entity-specific display context to each member map."
  [db entity-type clusters]
  (let [config (get-entity-config! entity-type)
        fk-tables (:fk-tables config)
        all-ids (->> clusters
                  (mapcat :members)
                  (map :id)
                  distinct
                  vec)]
    (if (empty? all-ids)
      clusters
      (let [counts-by-id
            (if (empty? fk-tables)
              {}
              (reduce
                (fn [acc [fk-table {:keys [col]}]]
                  (let [rows (jdbc/execute!
                               db
                               (sql/format {:select [[col :entity_id]
                                                     [[:count :*] :cnt]]
                                            :from [(keyword (name fk-table))]
                                            :where [:in col all-ids]
                                            :group-by [col]})
                               {:builder-fn rs/as-unqualified-lower-maps})]
                    (reduce
                      (fn [a {:keys [entity_id cnt]}]
                        (update a entity_id (fnil + 0) cnt))
                      acc
                      rows)))
                {}
                fk-tables))
            context-by-id (contextual-info-by-id db entity-type all-ids)]
        (mapv
          (fn [cluster]
            (update cluster :members
              (fn [members]
                (mapv (fn [member]
                        (merge member
                          {:usage-count (get counts-by-id (:id member) 0)}
                          (get context-by-id (:id member) {})))
                  members))))
          clusters)))))

(comment
  ;; REPL usage examples
  ;; (detect-duplicates db :suppliers :prefix {:prefix-words 2})
  ;; (detect-duplicates db :articles :trigram {:threshold 0.5})
  ;; (detect-duplicates db :manufacturers :levenshtein {:max-distance 2})
  ;; (enrich-with-usage-counts db :suppliers clusters)
  :rcf)
