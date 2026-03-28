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
    [app.domain.backend.expenses.services.service-configs.normalization :as normalize]
    [buddy.core.codecs :as codecs]
    [buddy.core.hash :as hash]
    [clojure.set :as set]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

;; ============================================================================
;; Entity Configuration
;; ============================================================================

(def ^:private entity-configs
  "Per-entity config for duplicate detection.

  :group-col (optional) — when set, candidates are only compared within the
  same group (e.g. stores must share the same supplier_id to be considered
  duplicates). Applies to all strategies.

  :normalize-fn (optional) — derive a detection key from the display name when
  the entity does not persist a normalized_key column."
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
                   :group-col :unit
                   :fk-tables {:expense_items {:col :article_id}
                               :article_aliases {:col :article_id}}}
   :stores        {:table "stores"
                   :name-col :display_name
                   :key-col :normalized_key
                   :group-col :supplier_id
                   :fk-tables {:expenses {:col :store_id}
                               :store_aliases {:col :store_id}}}
   :manufacturers {:table "manufacturers"
                   :name-col :display_name
                   :key-col :normalized_key
                   :fk-tables {:articles {:col :manufacturer_id}}}
   :subcategories {:table "subcategories"
                   :name-col :name
                   :normalize-fn normalize/normalize-store-key
                   :fk-tables {:articles {:col :subcategory_id}}}})

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
  [{:keys [name-col key-col group-col]}]
  (cond-> [:id name-col :created_at]
    key-col (conj key-col)
    group-col (conj group-col)))

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
  [rows pairs {:keys [limit max-cluster-size] :or {limit 50 max-cluster-size 10}}]
  (let [all-ids (distinct (concat (map :id_a pairs) (map :id_b pairs)))
        uf (make-union-find all-ids)]
    (doseq [{:keys [id_a id_b]} pairs]
      (uf-union uf id_a id_b))
    (let [clusters (uf-clusters uf)
          id->row (zipmap (map :id rows) rows)]
      (->> clusters
        (filter #(<= (count %) max-cluster-size))
        (mapv (fn [ids]
                {:members (->> ids
                            (mapv id->row)
                            (remove nil?)
                            vec)
                 :count   (count ids)}))
        (sort-by #(- (:count %)))
        (take limit)
        vec))))

(defn- same-group?
  [group-col row-a row-b]
  (or (nil? group-col)
    (= (get row-a group-col) (get row-b group-col))))

(defn- trigram-set
  [s]
  (let [value (str "  " (or s "") "  ")]
    (if (<= (count value) 3)
      #{value}
      (->> (range 0 (- (count value) 2))
        (map (fn [idx] (subs value idx (+ idx 3))))
        set))))

(defn- trigram-similarity
  [a b]
  (let [ta (trigram-set a)
        tb (trigram-set b)
        denom (+ (count ta) (count tb))]
    (if (zero? denom)
      0.0
      (/ (* 2.0 (count (set/intersection ta tb))) denom))))

(defn- levenshtein-distance
  [a b]
  (let [a (vec (or a ""))
        b (vec (or b ""))
        n (count a)
        m (count b)]
    (cond
      (zero? n) m
      (zero? m) n
      :else
      (loop [i 1
             prev (vec (range (inc m)))]
        (if (> i n)
          (peek prev)
          (let [curr (loop [j 1
                            row [i]]
                       (if (> j m)
                         (vec row)
                         (let [cost (if (= (nth a (dec i)) (nth b (dec j))) 0 1)
                               deletion (inc (nth prev j))
                               insertion (inc (peek row))
                               substitution (+ (nth prev (dec j)) cost)]
                           (recur (inc j) (conj row (min deletion insertion substitution))))))]
            (recur (inc i) curr)))))))

(defn- detect-similar-pairs-in-memory
  [rows group-col score-fn matches?]
  (let [rows* (vec rows)
        total (count rows*)]
    (reduce
      (fn [acc idx]
        (let [row-a (nth rows* idx)
              key-a (:normalized_key row-a)]
          (if-not key-a
            acc
            (reduce
              (fn [acc2 jdx]
                (let [row-b (nth rows* jdx)
                      key-b (:normalized_key row-b)
                      score (when (and key-b (same-group? group-col row-a row-b))
                              (score-fn key-a key-b))]
                  (if (and score (matches? score))
                    (conj acc2 {:id_a (:id row-a)
                                :id_b (:id row-b)})
                    acc2)))
              acc
              (range (inc idx) total)))))
      []
      (range total))))

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

(defn- subcategory-category-names-by-id
  [db all-ids]
  (->> (jdbc/execute!
         db
         (sql/format {:select [[:sc.id :entity_id]
                               [:c.name :category_name]]
                      :from [[:subcategories :sc]]
                      :join [[:categories :c] [:= :c.id :sc.category_id]]
                      :where [:in :sc.id all-ids]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (reduce (fn [acc {:keys [entity_id category_name]}]
              (assoc acc entity_id {:category-name category_name}))
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

    :subcategories
    (subcategory-category-names-by-id db all-ids)

    {}))

(defn enrich-members-with-context
  "Attach entity-specific display context to standalone candidate rows."
  [db entity-type members]
  (let [all-ids (->> members
                  (map :id)
                  distinct
                  vec)]
    (if (empty? all-ids)
      (vec members)
      (let [context-by-id (contextual-info-by-id db entity-type all-ids)]
        (mapv (fn [member]
                (merge member (get context-by-id (:id member) {})))
          members)))))

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
